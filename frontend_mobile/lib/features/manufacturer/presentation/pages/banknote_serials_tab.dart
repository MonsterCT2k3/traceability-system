import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';

import '../../../../core/network/api_client.dart';
import '../../utils/banknote_serial_candidate_parser.dart';
import 'banknote_barcode_scan_page.dart';
import 'banknote_live_ocr_page.dart';

const _kPrefsKey = 'manufacturer_banknote_serials_json';

/// Seri lưu theo **tập** (camera: `kBanknoteOcrBatchSize` seri/tập). Nhập tay / quét: một tập 1 seri.
class BanknoteSerialsTab extends StatefulWidget {
  const BanknoteSerialsTab({super.key});

  @override
  State<BanknoteSerialsTab> createState() => _BanknoteSerialsTabState();
}

class _BanknoteSerialsTabState extends State<BanknoteSerialsTab> {
  final _controller = TextEditingController();
  List<List<String>> _batches = [];
  bool _loading = true;
  bool _syncing = false;

  ApiClient get _api => GetIt.I<ApiClient>();

  int get _totalSerials => _batches.fold<int>(0, (s, b) => s + b.length);

  Set<String> _allSerialsSet() {
    final out = <String>{};
    for (final b in _batches) {
      for (final x in b) {
        out.add(x);
      }
    }
    return out;
  }

  /// Gửi lên API: từng seri một (thứ tự theo tập trên máy).
  List<String> _flattenSerialsForSync() {
    final out = <String>[];
    final seen = <String>{};
    for (final b in _batches) {
      for (final s in b) {
        if (seen.add(s)) out.add(s);
      }
    }
    return out;
  }

  Future<void> _syncToServer() async {
    if (_batches.isEmpty || _syncing) return;
    final serials = _flattenSerialsForSync();
    if (serials.isEmpty) return;

    setState(() => _syncing = true);
    try {
      final res = await _api.post(
        '/product/api/v1/banknote-serials/bulk',
        data: <String, dynamic>{'serials': serials},
      );
      final raw = res.data;
      if (raw is! Map) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Phản hồi server không hợp lệ.')),
          );
        }
        return;
      }
      final code = raw['code'];
      if (code != 200) {
        final msg = raw['message']?.toString() ?? 'Lỗi $code';
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
        return;
      }
      final result = raw['result'];
      var inserted = 0;
      var skippedDup = 0;
      var skippedInv = 0;
      if (result is Map) {
        inserted = (result['inserted'] as num?)?.toInt() ?? 0;
        skippedDup = (result['skippedDuplicate'] as num?)?.toInt() ?? 0;
        skippedInv = (result['skippedInvalid'] as num?)?.toInt() ?? 0;
      }
      setState(() => _batches = []);
      await _persist();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Đã lưu DB: thêm $inserted seri'
            '${skippedDup > 0 ? ', bỏ qua trùng $skippedDup' : ''}'
            '${skippedInv > 0 ? ', không hợp lệ $skippedInv' : ''}. Đã xóa danh sách trên máy.',
          ),
        ),
      );
    } on DioException catch (e) {
      final msg = e.response?.data is Map
          ? (e.response!.data as Map)['message']?.toString()
          : e.message;
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(msg ?? 'Lỗi mạng hoặc máy chủ')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    } finally {
      if (mounted) setState(() => _syncing = false);
    }
  }

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  List<List<String>> _migrateFlatListToBatches(List<dynamic> flat) {
    final strings = flat.map((e) => e.toString()).toList();
    const chunk = kBanknoteOcrBatchSize;
    final out = <List<String>>[];
    for (var i = 0; i < strings.length; i += chunk) {
      final end = i + chunk > strings.length ? strings.length : i + chunk;
      out.add(strings.sublist(i, end));
    }
    return out;
  }

  Future<void> _load() async {
    final raw = _api.sharedPreferences.getString(_kPrefsKey);
    if (raw != null && raw.isNotEmpty) {
      try {
        final decoded = jsonDecode(raw);
        if (decoded is Map) {
          final batches = decoded['batches'];
          if (batches is List) {
            final parsed = <List<String>>[];
            for (final e in batches) {
              if (e is List) {
                parsed.add(e.map((x) => x.toString()).toList());
              }
            }
            setState(() {
              _batches = parsed;
              _loading = false;
            });
            return;
          }
        }
        if (decoded is List) {
          if (decoded.isNotEmpty && decoded.first is List) {
            final parsed = <List<String>>[];
            for (final e in decoded) {
              if (e is List) {
                parsed.add(e.map((x) => x.toString()).toList());
              }
            }
            setState(() {
              _batches = parsed;
              _loading = false;
            });
            return;
          }
          setState(() {
            _batches = _migrateFlatListToBatches(decoded);
            _loading = false;
          });
          return;
        }
      } catch (_) {}
    }
    setState(() => _loading = false);
  }

  Future<void> _persist() async {
    await _api.sharedPreferences.setString(
      _kPrefsKey,
      jsonEncode({
        'v': 2,
        'batches': _batches,
      }),
    );
  }

  Future<void> _add(String raw) async {
    final n = normalizeBanknoteSerialStored(raw);
    if (n == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Seri không hợp lệ (4–32 ký tự: chữ, số, gạch ngang).')),
      );
      return;
    }
    if (_allSerialsSet().contains(n)) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Đã có seri $n trong dữ liệu đã lưu')),
      );
      return;
    }
    setState(() => _batches = [..._batches, [n]]);
    await _persist();
    _controller.clear();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Đã thêm tập 1 seri: $n (tổng $_totalSerials seri, ${_batches.length} tập)')),
    );
  }

  Future<void> _addOcrBatch(List<String> batch) async {
    if (batch.length != kBanknoteOcrBatchSize) return;
    final normalized = <String>[];
    final seen = <String>{};
    for (final raw in batch) {
      final n = normalizeBanknoteSerialStored(raw);
      if (n == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Trong tập có seri không hợp lệ — không lưu.')),
        );
        return;
      }
      if (!seen.add(n)) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Trong tập bị trùng: $n — không lưu.')),
        );
        return;
      }
      normalized.add(n);
    }
    final existing = _allSerialsSet();
    for (final n in normalized) {
      if (existing.contains(n)) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Seri $n đã có trong dữ liệu — không lưu tập.')),
        );
        return;
      }
    }
    setState(() => _batches = [..._batches, normalized]);
    await _persist();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Đã lưu tập $kBanknoteOcrBatchSize seri (${_batches.length} tập, $_totalSerials seri).')),
    );
  }

  Future<void> _removeBatch(int batchIndex) async {
    setState(() {
      _batches = List<List<String>>.from(_batches)..removeAt(batchIndex);
    });
    await _persist();
  }

  Future<void> _clearAll() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Xóa toàn bộ?'),
        content: const Text('Mọi tập seri trên máy sẽ bị xoá.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Hủy')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Xóa')),
        ],
      ),
    );
    if (ok == true) {
      setState(() => _batches = []);
      await _persist();
    }
  }

  Future<void> _openScanner() async {
    final value = await Navigator.of(context).push<String>(
      MaterialPageRoute(builder: (_) => const BanknoteBarcodeScanPage()),
    );
    if (value != null && value.isNotEmpty) {
      await _add(value);
    }
  }

  Future<void> _openLiveOcr() async {
    final batch = await Navigator.of(context).push<List<String>>(
      MaterialPageRoute(builder: (_) => const BanknoteLiveOcrPage()),
    );
    if (batch != null && batch.length == kBanknoteOcrBatchSize) {
      await _addOcrBatch(batch);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
          child: Text(
            'Lưu theo tập: camera + nhận diện mỗi lần = một tập $kBanknoteOcrBatchSize seri. Nhập tay / quét mã = một tập 1 seri.',
            style: TextStyle(fontSize: 13, color: Colors.grey.shade700),
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _controller,
                  decoration: const InputDecoration(
                    labelText: 'Nhập số seri',
                    hintText: 'VD: AB12345678',
                    border: OutlineInputBorder(),
                    isDense: true,
                  ),
                  textCapitalization: TextCapitalization.characters,
                  onSubmitted: _add,
                ),
              ),
              const SizedBox(width: 8),
              FilledButton(
                onPressed: () => _add(_controller.text),
                child: const Text('Thêm'),
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              FilledButton.icon(
                onPressed: _openLiveOcr,
                icon: const Icon(Icons.document_scanner_outlined),
                label: const Text('Camera + nhận diện ($kBanknoteOcrBatchSize seri / tập)'),
              ),
              const SizedBox(height: 8),
              OutlinedButton.icon(
                onPressed: _openScanner,
                icon: const Icon(Icons.qr_code_scanner),
                label: const Text('Quét mã vạch / QR'),
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '$_totalSerials seri · ${_batches.length} tập',
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
              if (_batches.isNotEmpty)
                TextButton(onPressed: _clearAll, child: const Text('Xóa hết')),
            ],
          ),
        ),
        if (_batches.isNotEmpty)
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                FilledButton.icon(
                  onPressed: _syncing ? null : _syncToServer,
                  icon: _syncing
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.cloud_upload_outlined),
                  label: Text(_syncing ? 'Đang lưu lên server…' : 'Lưu lên server (DB) — gửi hết $_totalSerials seri'),
                ),
                const SizedBox(height: 4),
                Text(
                  'Mỗi seri một bản ghi trên DB; tập trên máy chỉ để chụp cho tiện. Seri đã có trên hệ thống sẽ bị bỏ qua.',
                  style: TextStyle(fontSize: 11, color: Colors.grey.shade700),
                ),
              ],
            ),
          ),
        const Divider(height: 1),
        Expanded(
          child: _batches.isEmpty
              ? Center(
                  child: Text(
                    'Chưa có tập nào.\nDùng Camera + nhận diện ($kBanknoteOcrBatchSize seri) hoặc nhập / quét mã.',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: Colors.grey.shade600),
                  ),
                )
              : ListView.builder(
                  padding: const EdgeInsets.only(bottom: 88),
                  itemCount: _batches.length,
                  itemBuilder: (_, bi) {
                    final batch = _batches[bi];
                    return Card(
                      margin: const EdgeInsets.fromLTRB(12, 0, 12, 10),
                      child: ExpansionTile(
                        initiallyExpanded: bi == _batches.length - 1,
                        title: Row(
                          children: [
                            Expanded(
                              child: Text(
                                'Tập ${bi + 1} · ${batch.length} seri',
                                style: const TextStyle(fontWeight: FontWeight.w600),
                              ),
                            ),
                            IconButton(
                              tooltip: 'Xóa cả tập',
                              icon: const Icon(Icons.delete_outline),
                              onPressed: () => _removeBatch(bi),
                            ),
                          ],
                        ),
                        children: [
                          for (var si = 0; si < batch.length; si++)
                            ListTile(
                              dense: true,
                              leading: CircleAvatar(
                                radius: 14,
                                backgroundColor: Colors.green.shade50,
                                child: Text(
                                  '${si + 1}',
                                  style: TextStyle(fontSize: 11, color: Colors.green.shade800),
                                ),
                              ),
                              title: SelectableText(
                                batch[si],
                                style: const TextStyle(
                                  fontWeight: FontWeight.w500,
                                  fontFamily: 'monospace',
                                ),
                              ),
                            ),
                        ],
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }
}

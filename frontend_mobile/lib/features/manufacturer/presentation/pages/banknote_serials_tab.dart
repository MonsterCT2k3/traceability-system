import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../../../core/network/api_client.dart';
import '../../utils/banknote_serial_candidate_parser.dart';
import 'banknote_live_ocr_page.dart';

const _kPrefsKey = 'manufacturer_banknote_serials_json';

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
  SharedPreferences get _prefs => GetIt.I<SharedPreferences>();

  int get _totalSerials =>
      _batches.fold<int>(0, (sum, batch) => sum + batch.length);

  Set<String> _allSerialsSet() {
    final out = <String>{};
    for (final batch in _batches) {
      out.addAll(batch);
    }
    return out;
  }

  List<String> _flattenSerialsForSync() {
    final out = <String>[];
    final seen = <String>{};
    for (final batch in _batches) {
      for (final serial in batch) {
        if (seen.add(serial)) out.add(serial);
      }
    }
    return out;
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
    final out = <List<String>>[];
    for (var i = 0; i < strings.length; i += kBanknoteOcrBatchSize) {
      final end = i + kBanknoteOcrBatchSize > strings.length
          ? strings.length
          : i + kBanknoteOcrBatchSize;
      out.add(strings.sublist(i, end));
    }
    return out;
  }

  Future<void> _load() async {
    final raw = _prefs.getString(_kPrefsKey);
    if (raw != null && raw.isNotEmpty) {
      try {
        final decoded = jsonDecode(raw);
        if (decoded is Map && decoded['batches'] is List) {
          final parsed = <List<String>>[];
          for (final entry in decoded['batches'] as List) {
            if (entry is List) {
              parsed.add(entry.map((value) => value.toString()).toList());
            }
          }
          setState(() {
            _batches = parsed;
            _loading = false;
          });
          return;
        }
        if (decoded is List) {
          final parsed = decoded.isNotEmpty && decoded.first is List
              ? decoded
                  .whereType<List>()
                  .map((entry) =>
                      entry.map((value) => value.toString()).toList())
                  .toList()
              : _migrateFlatListToBatches(decoded);
          setState(() {
            _batches = parsed;
            _loading = false;
          });
          return;
        }
      } catch (_) {}
    }
    setState(() => _loading = false);
  }

  Future<void> _persist() async {
    await _prefs.setString(
      _kPrefsKey,
      jsonEncode({'v': 2, 'batches': _batches}),
    );
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
        final message = raw['message']?.toString() ?? 'Lỗi $code';
        if (mounted) {
          ScaffoldMessenger.of(context)
              .showSnackBar(SnackBar(content: Text(message)));
        }
        return;
      }
      final result = raw['result'];
      var inserted = 0;
      var skippedDuplicate = 0;
      var skippedInvalid = 0;
      if (result is Map) {
        inserted = (result['inserted'] as num?)?.toInt() ?? 0;
        skippedDuplicate = (result['skippedDuplicate'] as num?)?.toInt() ?? 0;
        skippedInvalid = (result['skippedInvalid'] as num?)?.toInt() ?? 0;
      }
      setState(() => _batches = []);
      await _persist();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Đã lưu DB: thêm $inserted seri'
            '${skippedDuplicate > 0 ? ', bỏ qua trùng $skippedDuplicate' : ''}'
            '${skippedInvalid > 0 ? ', không hợp lệ $skippedInvalid' : ''}. '
            'Đã xóa danh sách trên máy.',
          ),
        ),
      );
    } on DioException catch (e) {
      final message = e.response?.data is Map
          ? (e.response!.data as Map)['message']?.toString()
          : e.message;
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(message ?? 'Lỗi mạng hoặc máy chủ')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    } finally {
      if (mounted) setState(() => _syncing = false);
    }
  }

  Future<void> _add(String raw) async {
    final serial = normalizeBanknoteSerialStored(raw);
    if (serial == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Seri không hợp lệ (4-32 ký tự: chữ, số, gạch ngang).'),
        ),
      );
      return;
    }
    if (_allSerialsSet().contains(serial)) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Đã có seri $serial trong dữ liệu đã lưu')),
      );
      return;
    }
    setState(() => _batches = [
          ..._batches,
          [serial]
        ]);
    await _persist();
    _controller.clear();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Đã thêm seri $serial vào danh sách chờ đồng bộ')),
    );
  }

  Future<void> _addOcrBatch(List<String> batch) async {
    if (batch.length != kBanknoteOcrBatchSize) return;
    final normalized = <String>[];
    final seen = <String>{};
    for (final raw in batch) {
      final serial = normalizeBanknoteSerialStored(raw);
      if (serial == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
              content: Text('Trong tập có seri không hợp lệ, không thể lưu.')),
        );
        return;
      }
      if (!seen.add(serial)) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content: Text('Trong tập bị trùng seri $serial, không thể lưu.')),
        );
        return;
      }
      normalized.add(serial);
    }
    final existing = _allSerialsSet();
    for (final serial in normalized) {
      if (existing.contains(serial)) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
              content:
                  Text('Seri $serial đã có trong dữ liệu, không lưu tập.')),
        );
        return;
      }
    }
    setState(() => _batches = [..._batches, normalized]);
    await _persist();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Đã lưu tập $kBanknoteOcrBatchSize seri để đồng bộ.'),
      ),
    );
  }

  Future<void> _removeBatch(int batchIndex) async {
    setState(() =>
        _batches = List<List<String>>.from(_batches)..removeAt(batchIndex));
    await _persist();
  }

  Future<void> _clearAll() async {
    final accepted = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Xóa toàn bộ dữ liệu chờ?'),
        content: const Text('Mọi tập seri đang lưu trên thiết bị sẽ bị xóa.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('Xóa'),
          ),
        ],
      ),
    );
    if (accepted == true) {
      setState(() => _batches = []);
      await _persist();
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
    if (_loading) return const Center(child: CircularProgressIndicator());

    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 92),
      children: [
        _buildSummary(),
        const SizedBox(height: 14),
        _buildCapturePanel(),
        if (_batches.isNotEmpty) ...[
          const SizedBox(height: 14),
          _buildSyncPanel(),
        ],
        const SizedBox(height: 20),
        _buildListHeading(),
        const SizedBox(height: 10),
        if (_batches.isEmpty)
          const _EmptySerialState()
        else
          ..._buildBatchCards(),
      ],
    );
  }

  Widget _buildSummary() {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF103B4B), Color(0xFF087B69)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(22),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.confirmation_number_outlined, color: Colors.white),
              SizedBox(width: 9),
              Text(
                'Kho seri sản phẩm',
                style: TextStyle(
                    color: Colors.white,
                    fontSize: 18,
                    fontWeight: FontWeight.w700),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              _Metric(label: 'Tổng seri', value: '$_totalSerials'),
              const SizedBox(width: 10),
              _Metric(label: 'Tập chờ lưu', value: '${_batches.length}'),
            ],
          ),
          const SizedBox(height: 13),
          const Text(
            'Quét tiền mẫu hoặc nhập tay, sau đó lưu toàn bộ lên hệ thống.',
            style: TextStyle(color: Colors.white70, fontSize: 12),
          ),
        ],
      ),
    );
  }

  Widget _buildCapturePanel() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(19),
        border: Border.all(color: const Color(0xFFE2E8E7)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Thêm seri mới',
            style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
          ),
          const SizedBox(height: 5),
          Text(
            'Nhập thủ công một seri hoặc quét $kBanknoteOcrBatchSize seri trong một lần.',
            style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _controller,
                  textCapitalization: TextCapitalization.characters,
                  onSubmitted: _add,
                  decoration: const InputDecoration(
                    labelText: 'Số seri',
                    hintText: 'VD: AB12345678',
                    prefixIcon: Icon(Icons.numbers_rounded),
                    border: OutlineInputBorder(),
                  ),
                ),
              ),
              const SizedBox(width: 9),
              FilledButton(
                style: FilledButton.styleFrom(
                  minimumSize: const Size(58, 56),
                  padding: const EdgeInsets.symmetric(horizontal: 14),
                ),
                onPressed: () => _add(_controller.text),
                child: const Text('Thêm'),
              ),
            ],
          ),
          const SizedBox(height: 11),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              style: FilledButton.styleFrom(
                backgroundColor: const Color(0xFF087B69),
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(13)),
              ),
              onPressed: _openLiveOcr,
              icon: const Icon(Icons.document_scanner_outlined),
              label: const Text('Mở camera nhận diện seri'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSyncPanel() {
    return Container(
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: const Color(0xFFECFDF5),
        border: Border.all(color: const Color(0xFFA7F3D0)),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Sẵn sàng đồng bộ',
            style: TextStyle(
                fontWeight: FontWeight.w700, color: Color(0xFF065F46)),
          ),
          const SizedBox(height: 5),
          Text(
            '$_totalSerials seri đang lưu trên thiết bị sẽ được gửi lên cơ sở dữ liệu.',
            style: const TextStyle(fontSize: 12, color: Color(0xFF477568)),
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: _syncing ? null : _syncToServer,
              icon: _syncing
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.cloud_upload_outlined),
              label:
                  Text(_syncing ? 'Đang lưu lên server...' : 'Lưu lên server'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildListHeading() {
    return Row(
      children: [
        const Expanded(
          child: Text(
            'Danh sách chờ đồng bộ',
            style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
          ),
        ),
        if (_batches.isNotEmpty)
          TextButton.icon(
            onPressed: _clearAll,
            icon: const Icon(Icons.delete_sweep_outlined, size: 19),
            label: const Text('Xóa hết'),
          ),
      ],
    );
  }

  List<Widget> _buildBatchCards() {
    return List.generate(_batches.length, (batchIndex) {
      final batch = _batches[batchIndex];
      return Card(
        margin: const EdgeInsets.only(bottom: 10),
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(17),
          side: BorderSide(color: Colors.grey.shade200),
        ),
        child: ExpansionTile(
          initiallyExpanded: batchIndex == _batches.length - 1,
          shape: const Border(),
          collapsedShape: const Border(),
          tilePadding: const EdgeInsets.fromLTRB(15, 4, 8, 4),
          leading: CircleAvatar(
            backgroundColor: const Color(0xFFE6F5F2),
            child: Text(
              '${batchIndex + 1}',
              style: const TextStyle(
                  color: Color(0xFF087B69), fontWeight: FontWeight.bold),
            ),
          ),
          title: Text(
            'Tập ${batchIndex + 1}',
            style: const TextStyle(fontWeight: FontWeight.w700),
          ),
          subtitle: Text('${batch.length} seri'),
          trailing: IconButton(
            tooltip: 'Xóa tập này',
            icon: const Icon(Icons.delete_outline_rounded),
            onPressed: () => _removeBatch(batchIndex),
          ),
          children: [
            for (var serialIndex = 0; serialIndex < batch.length; serialIndex++)
              ListTile(
                dense: true,
                contentPadding: const EdgeInsets.fromLTRB(21, 0, 18, 0),
                leading: Text(
                  '${serialIndex + 1}.',
                  style: TextStyle(color: Colors.grey.shade500),
                ),
                title: SelectableText(
                  batch[serialIndex],
                  style: const TextStyle(
                      fontWeight: FontWeight.w600, fontFamily: 'monospace'),
                ),
              ),
            const SizedBox(height: 6),
          ],
        ),
      );
    });
  }
}

class _Metric extends StatelessWidget {
  const _Metric({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 11),
        decoration: BoxDecoration(
          color: Colors.white.withOpacity(0.14),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              value,
              style: const TextStyle(
                  color: Colors.white,
                  fontSize: 22,
                  fontWeight: FontWeight.w700),
            ),
            Text(label,
                style: const TextStyle(color: Colors.white70, fontSize: 12)),
          ],
        ),
      ),
    );
  }
}

class _EmptySerialState extends StatelessWidget {
  const _EmptySerialState();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 42),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAF9),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Column(
        children: [
          Icon(Icons.qr_code_2_rounded, color: Colors.grey.shade300, size: 52),
          const SizedBox(height: 12),
          const Text(
            'Chưa có seri chờ đồng bộ',
            style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
          ),
          const SizedBox(height: 6),
          Text(
            'Dùng camera nhận diện hoặc nhập số seri ở phía trên.',
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.grey.shade600),
          ),
        ],
      ),
    );
  }
}

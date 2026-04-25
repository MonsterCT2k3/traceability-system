import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../../../../../core/network/api_client.dart';

class ScanTab extends StatefulWidget {
  const ScanTab({super.key});

  @override
  State<ScanTab> createState() => _ScanTabState();
}

class _ScanTabState extends State<ScanTab> {
  final ApiClient _api = GetIt.I<ApiClient>();
  final MobileScannerController _scannerController = MobileScannerController(
    detectionSpeed: DetectionSpeed.noDuplicates,
  );

  bool _busy = false;
  bool _torchOn = false;
  String? _lastRaw;
  _TraceViewModel? _trace;
  String? _error;

  @override
  void dispose() {
    _scannerController.dispose();
    super.dispose();
  }

  Future<void> _onDetect(BarcodeCapture capture) async {
    if (_busy) return;
    final value = capture.barcodes.isNotEmpty ? capture.barcodes.first.rawValue : null;
    if (value == null || value.trim().isEmpty) return;
    final raw = value.trim();
    if (_lastRaw == raw) return;

    setState(() {
      _busy = true;
      _lastRaw = raw;
      _error = null;
      _trace = null;
    });

    await _scannerController.stop();

    try {
      final responseData = await _traceByQrContent(raw);
      final model = _TraceViewModel.fromApi(Map<String, dynamic>.from(responseData));
      if (!mounted) return;
      setState(() {
        _trace = model;
        _busy = false;
      });
    } on DioException catch (e) {
      if (!mounted) return;
      final msg = _readApiMessage(e) ?? 'Không truy xuất được sản phẩm';
      setState(() {
        _error = msg;
        _busy = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.toString();
        _busy = false;
      });
    }
  }

  Future<Map<String, dynamic>> _traceByQrContent(String raw) async {
    final serial = _extractSerial(raw);
    final unitId = _extractUnitId(raw);

    if (serial != null) {
      final res = await _api.get(
        '/product/api/v1/units/trace/by-serial',
        queryParameters: {'serial': serial},
      );
      return Map<String, dynamic>.from((res.data as Map)['result'] as Map);
    }

    if (unitId != null) {
      final res = await _api.get('/product/api/v1/units/$unitId/trace');
      return Map<String, dynamic>.from((res.data as Map)['result'] as Map);
    }

    // fallback: xem raw như serial
    final res = await _api.get(
      '/product/api/v1/units/trace/by-serial',
      queryParameters: {'serial': raw},
    );
    return Map<String, dynamic>.from((res.data as Map)['result'] as Map);
  }

  String? _extractSerial(String raw) {
    final serialParam = Uri.tryParse(raw)?.queryParameters['serial'];
    if (serialParam != null && serialParam.trim().isNotEmpty) {
      return serialParam.trim().toUpperCase();
    }
    final normalized = raw.trim().toUpperCase();
    final isLikelySerial = RegExp(r'^[A-Z0-9\-]{4,64}$').hasMatch(normalized);
    if (isLikelySerial && !_isUuid(normalized)) return normalized;
    return null;
  }

  String? _extractUnitId(String raw) {
    final uri = Uri.tryParse(raw);
    if (uri != null && uri.pathSegments.isNotEmpty) {
      final last = uri.pathSegments.last.trim();
      if (_isUuid(last)) return last;
    }
    final plain = raw.trim();
    if (_isUuid(plain)) return plain;
    return null;
  }

  bool _isUuid(String s) {
    return RegExp(
      r'^[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}$',
    ).hasMatch(s.trim());
  }

  Future<void> _scanAgain() async {
    setState(() {
      _trace = null;
      _error = null;
      _lastRaw = null;
    });
    await _scannerController.start();
  }

  String? _readApiMessage(DioException e) {
    final d = e.response?.data;
    if (d is Map && d['message'] != null) return '${d['message']}';
    return e.message;
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Expanded(
          flex: 6,
          child: Stack(
            children: [
              MobileScanner(
                controller: _scannerController,
                onDetect: _onDetect,
              ),
              Positioned(
                top: 12,
                right: 12,
                child: FilledButton.tonalIcon(
                  onPressed: () async {
                    await _scannerController.toggleTorch();
                    setState(() => _torchOn = !_torchOn);
                  },
                  icon: Icon(_torchOn ? Icons.flash_on : Icons.flash_off),
                  label: const Text('Đèn'),
                ),
              ),
              if (_busy)
                const Center(
                  child: CircularProgressIndicator(),
                ),
            ],
          ),
        ),
        Expanded(
          flex: 4,
          child: Container(
            width: double.infinity,
            color: Theme.of(context).colorScheme.surface,
            padding: const EdgeInsets.fromLTRB(16, 14, 16, 12),
            child: _buildBottomPanel(context),
          ),
        ),
      ],
    );
  }

  Widget _buildBottomPanel(BuildContext context) {
    if (_error != null) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Kết quả truy xuất', style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Text(_error!, style: const TextStyle(color: Colors.redAccent)),
          const SizedBox(height: 12),
          FilledButton.icon(
            onPressed: _scanAgain,
            icon: const Icon(Icons.qr_code_scanner),
            label: const Text('Quét lại'),
          ),
        ],
      );
    }

    if (_trace == null) {
      return const Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Quét QR truy xuất', style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
          SizedBox(height: 8),
          Text('Hướng camera vào QR trên sản phẩm để xem thông tin truy xuất.'),
          SizedBox(height: 6),
          Text('Mỗi lần truy xuất sẽ tăng scanCount.', style: TextStyle(color: Colors.grey)),
        ],
      );
    }

    final t = _trace!;
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Expanded(
                child: Text('Kết quả truy xuất', style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
              ),
              Chip(
                label: Text('Lượt quét: ${t.scanCount}'),
                avatar: const Icon(Icons.remove_red_eye_outlined, size: 18),
              ),
            ],
          ),
          const SizedBox(height: 8),
          _line('Sản phẩm', t.productName),
          _line('Seri', t.unitSerial),
          _line('Mã đơn vị', t.unitId),
          _line('Mã thùng', t.cartonCode),
          _line('Mã lô', t.palletCode),
          _line('Tên lô', t.palletName),
          _line('Ngày sản xuất', t.palletManufacturedAt),
          _line('Hạn dùng', t.palletExpiryAt ?? '—'),
          if ((t.productDescription ?? '').trim().isNotEmpty) _line('Mô tả', t.productDescription!.trim()),
          const SizedBox(height: 10),
          FilledButton.icon(
            onPressed: _scanAgain,
            icon: const Icon(Icons.qr_code_scanner),
            label: const Text('Quét mã khác'),
          ),
        ],
      ),
    );
  }

  Widget _line(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: RichText(
        text: TextSpan(
          style: const TextStyle(color: Colors.black87, fontSize: 13.5),
          children: [
            TextSpan(text: '$label: ', style: const TextStyle(fontWeight: FontWeight.w700)),
            TextSpan(text: value),
          ],
        ),
      ),
    );
  }
}

class _TraceViewModel {
  final String unitId;
  final String unitSerial;
  final String productName;
  final String? productDescription;
  final String cartonCode;
  final String palletCode;
  final String palletName;
  final String palletManufacturedAt;
  final String? palletExpiryAt;
  final int scanCount;

  _TraceViewModel({
    required this.unitId,
    required this.unitSerial,
    required this.productName,
    required this.productDescription,
    required this.cartonCode,
    required this.palletCode,
    required this.palletName,
    required this.palletManufacturedAt,
    required this.palletExpiryAt,
    required this.scanCount,
  });

  factory _TraceViewModel.fromApi(Map<String, dynamic> json) {
    return _TraceViewModel(
      unitId: json['unitId']?.toString() ?? '',
      unitSerial: json['unitSerial']?.toString() ?? '',
      productName: json['productName']?.toString() ?? 'Không rõ',
      productDescription: json['productDescription']?.toString(),
      cartonCode: json['cartonCode']?.toString() ?? '',
      palletCode: json['palletCode']?.toString() ?? '',
      palletName: json['palletName']?.toString() ?? '',
      palletManufacturedAt: json['palletManufacturedAt']?.toString() ?? '',
      palletExpiryAt: json['palletExpiryAt']?.toString(),
      scanCount: json['scanCount'] is int
          ? json['scanCount'] as int
          : int.tryParse('${json['scanCount']}') ?? 0,
    );
  }
}

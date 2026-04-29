import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:image_picker/image_picker.dart';
import 'package:google_mlkit_barcode_scanning/google_mlkit_barcode_scanning.dart' as mlkit;

import '../../../../../core/network/api_client.dart';
import '../trace_result_page.dart';

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

  @override
  void dispose() {
    _scannerController.dispose();
    super.dispose();
  }

  Future<void> _onDetect(BarcodeCapture capture) async {
    if (_busy) return;
    final value = capture.barcodes.isNotEmpty ? capture.barcodes.first.rawValue : null;
    if (value == null || value.trim().isEmpty) return;
    await _processBarcode(value.trim());
  }

  Future<void> _processBarcode(String raw) async {
    if (_busy) return;
    setState(() {
      _busy = true;
    });
    
    await _scannerController.stop();

    try {
      final responseData = await _traceByQrContent(raw);
      final model = TraceViewModel.fromApi(Map<String, dynamic>.from(responseData));
      
      if (!mounted) return;
      setState(() => _busy = false);
      
      await Navigator.push(
        context,
        MaterialPageRoute(builder: (context) => TraceResultPage(trace: model)),
      );
      
      if (mounted) {
        await _scannerController.start();
      }
    } on DioException catch (e) {
      if (!mounted) return;
      setState(() => _busy = false);
      final msg = _readApiMessage(e) ?? 'Không truy xuất được sản phẩm';
      _showError(msg);
    } catch (e) {
      if (!mounted) return;
      setState(() => _busy = false);
      _showError(e.toString());
    }
  }

  void _showError(String msg) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (c) => AlertDialog(
        title: const Text('Lỗi truy xuất'),
        content: Text(msg),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(c);
              _scannerController.start();
            },
            child: const Text('Đóng'),
          ),
        ],
      ),
    );
  }

  Future<void> _pickImage() async {
    final picker = ImagePicker();
    // Truyền maxWidth/maxHeight để image_picker tự apply EXIF rotation
    // vào pixels (ảnh trả về sẽ không còn EXIF rotation mà pixels đã đúng chiều)
    final xFile = await picker.pickImage(
      source: ImageSource.gallery,
      maxWidth: 1280,
      maxHeight: 1280,
    );
    if (xFile == null) return;

    setState(() => _busy = true);

    try {
      String? result;

      // Attempt 1: google_mlkit_barcode_scanning
      try {
        final inputImage = mlkit.InputImage.fromFilePath(xFile.path);
        final scanner = mlkit.BarcodeScanner();
        final barcodes = await scanner.processImage(inputImage);
        await scanner.close();
        if (barcodes.isNotEmpty && (barcodes.first.rawValue ?? '').isNotEmpty) {
          result = barcodes.first.rawValue;
        }
      } catch (_) {}

      // Attempt 2: fallback mobile_scanner.analyzeImage
      if (result == null || result.isEmpty) {
        try {
          final capture = await _scannerController.analyzeImage(xFile.path);
          if (capture != null && capture.barcodes.isNotEmpty) {
            final v = capture.barcodes.first.rawValue;
            if (v != null && v.isNotEmpty) result = v;
          }
        } catch (_) {}
      }

      if (result != null && result.isNotEmpty) {
        await _scannerController.stop();
        setState(() => _busy = false);
        await _processBarcode(result.trim());
        return;
      }

      setState(() => _busy = false);
      _showError('Không tìm thấy mã QR trong ảnh.');
    } catch (e) {
      setState(() => _busy = false);
      _showError('Lỗi khi phân tích ảnh: $e');
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

  String? _readApiMessage(DioException e) {
    final d = e.response?.data;
    if (d is Map && d['message'] != null) return '${d['message']}';
    return e.message;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          MobileScanner(
            controller: _scannerController,
            onDetect: _onDetect,
          ),
          // Overlay
          _buildOverlay(),
          
          // App bar layer
          SafeArea(
            child: Align(
              alignment: Alignment.topCenter,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    IconButton(
                      icon: const Icon(Icons.arrow_back_ios, color: Colors.white),
                      onPressed: () {
                        if (Navigator.canPop(context)) {
                          Navigator.pop(context);
                        }
                      },
                    ),
                    const Text(
                      'Quét QR',
                      style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.w600),
                    ),
                    IconButton(
                      icon: Icon(_torchOn ? Icons.flash_on : Icons.flash_off, color: Colors.white),
                      onPressed: () async {
                        await _scannerController.toggleTorch();
                        setState(() => _torchOn = !_torchOn);
                      },
                    ),
                  ],
                ),
              ),
            ),
          ),
          
          // Bottom buttons
          SafeArea(
            child: Align(
              alignment: Alignment.bottomCenter,
              child: Padding(
                padding: const EdgeInsets.only(bottom: 32.0),
                child: GestureDetector(
                  onTap: _pickImage,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: Colors.black45,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: const Icon(Icons.image_outlined, color: Colors.white, size: 28),
                      ),
                      const SizedBox(height: 8),
                      const Text('Tải ảnh', style: TextStyle(color: Colors.white, fontSize: 13)),
                    ],
                  ),
                ),
              ),
            ),
          ),

          if (_busy)
            Container(
              color: Colors.black54,
              child: const Center(
                child: CircularProgressIndicator(),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildOverlay() {
    return LayoutBuilder(
      builder: (context, constraints) {
        final double width = constraints.maxWidth;
        final double height = constraints.maxHeight;
        const double scanAreaSize = 260.0;
        
        final double borderHorizontal = (width - scanAreaSize) / 2;
        final double borderVertical = (height - scanAreaSize) / 2;

        return Stack(
          children: [
            Container(
              decoration: BoxDecoration(
                border: Border(
                  top: BorderSide(color: Colors.black87.withOpacity(0.6), width: borderVertical),
                  bottom: BorderSide(color: Colors.black87.withOpacity(0.6), width: borderVertical),
                  left: BorderSide(color: Colors.black87.withOpacity(0.6), width: borderHorizontal),
                  right: BorderSide(color: Colors.black87.withOpacity(0.6), width: borderHorizontal),
                ),
              ),
            ),
            Center(
              child: Container(
                width: scanAreaSize,
                height: scanAreaSize,
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.white, width: 2),
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}

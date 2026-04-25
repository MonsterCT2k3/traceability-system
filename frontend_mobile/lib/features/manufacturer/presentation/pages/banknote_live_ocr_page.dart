import 'dart:async';
import 'dart:collection';
import 'dart:io';

import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'package:image_picker/image_picker.dart';

import '../../utils/banknote_serial_candidate_parser.dart';

/// Camera + OCR: gom đủ `kBanknoteOcrBatchSize` seri một tập rồi `pop` `List<String>`.
class BanknoteLiveOcrPage extends StatefulWidget {
  const BanknoteLiveOcrPage({super.key});

  @override
  State<BanknoteLiveOcrPage> createState() => _BanknoteLiveOcrPageState();
}

class _BanknoteLiveOcrPageState extends State<BanknoteLiveOcrPage> {
  CameraController? _cam;
  TextRecognizer? _recognizer;
  Timer? _ocrTimer;
  bool _ready = false;
  String? _initError;
  bool _processing = false;

  final LinkedHashSet<String> _candidateSet = LinkedHashSet();
  List<String> get _candidates => _candidateSet.toList();

  /// Thứ tự chọn cho tập hiện tại (tối đa [kBanknoteOcrBatchSize], không trùng).
  final List<String> _chosenBatch = [];

  final _manual = TextEditingController();

  @override
  void initState() {
    super.initState();
    _start();
  }

  Future<void> _start() async {
    try {
      final all = await availableCameras();
      CameraDescription? back;
      for (final c in all) {
        if (c.lensDirection == CameraLensDirection.back) {
          back = c;
          break;
        }
      }
      back ??= all.isNotEmpty ? all.first : null;
      if (back == null) {
        setState(() => _initError = 'Không tìm thấy camera.');
        return;
      }
      final ctrl = CameraController(
        back,
        ResolutionPreset.high,
        enableAudio: false,
      );
      await ctrl.initialize();
      if (!mounted) {
        await ctrl.dispose();
        return;
      }
      _recognizer = TextRecognizer(script: TextRecognitionScript.latin);
      _cam = ctrl;
      setState(() => _ready = true);
      _ocrTimer = Timer.periodic(const Duration(milliseconds: 1200), (_) => _tickOcrFromPicture());
    } catch (e) {
      if (mounted) setState(() => _initError = 'Lỗi camera: $e');
    }
  }

  Future<void> _tickOcrFromPicture() async {
    final cam = _cam;
    final rec = _recognizer;
    if (!mounted || !_ready || cam == null || rec == null || !cam.value.isInitialized) return;
    if (_processing || cam.value.isTakingPicture) return;

    _processing = true;
    XFile? shot;
    try {
      shot = await cam.takePicture();
      final input = InputImage.fromFilePath(shot.path);
      final recognized = await rec.processImage(input);
      if (!mounted) return;
      final buf = StringBuffer();
      for (final block in recognized.blocks) {
        for (final line in block.lines) {
          buf.writeln(line.text);
        }
      }
      final found = parseBanknoteSerialCandidates(buf.toString());
      var changed = false;
      for (final f in found) {
        if (_candidateSet.add(f)) changed = true;
      }
      if (changed) setState(() {});
    } catch (_) {
      // Bỏ qua: máy bận hoặc lỗi tạm thời
    } finally {
      if (shot != null) {
        try {
          await File(shot.path).delete();
        } catch (_) {}
      }
      _processing = false;
    }
  }

  Future<void> _pickGalleryOnce() async {
    final x = await ImagePicker().pickImage(source: ImageSource.gallery, maxWidth: 2400, imageQuality: 90);
    if (x == null || _recognizer == null) return;
    try {
      final input = InputImage.fromFilePath(x.path);
      final recognized = await _recognizer!.processImage(input);
      final buf = StringBuffer();
      for (final block in recognized.blocks) {
        for (final line in block.lines) {
          buf.writeln(line.text);
        }
      }
      final found = parseBanknoteSerialCandidates(buf.toString());
      if (!mounted) return;
      var changed = false;
      for (final f in found) {
        if (_candidateSet.add(f)) changed = true;
      }
      if (changed) setState(() {});
      if (found.isEmpty && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Không đọc được seri từ ảnh — thử ảnh rõ hơn hoặc nhập tay.')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Lỗi: $e')));
      }
    }
  }

  void _toggleCandidateIntoBatch(String raw) {
    final n = normalizeBanknoteSerialStored(raw);
    if (n == null) return;
    final i = _chosenBatch.indexOf(n);
    if (i >= 0) {
      setState(() => _chosenBatch.removeAt(i));
      return;
    }
    if (_chosenBatch.length >= kBanknoteOcrBatchSize) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã đủ $kBanknoteOcrBatchSize seri — bỏ chọn một dòng hoặc lưu tập.')),
      );
      return;
    }
    setState(() => _chosenBatch.add(n));
  }

  void _addManualToBatch() {
    final n = normalizeBanknoteSerialStored(_manual.text);
    if (n == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Seri không hợp lệ (4–32 ký tự: chữ, số, gạch ngang).')),
      );
      return;
    }
    if (_chosenBatch.contains(n)) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Đã có $n trong tập.')));
      return;
    }
    if (_chosenBatch.length >= kBanknoteOcrBatchSize) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã đủ $kBanknoteOcrBatchSize seri — lưu tập hoặc bỏ chọn một dòng.')),
      );
      return;
    }
    setState(() {
      _chosenBatch.add(n);
      _manual.clear();
    });
  }

  void _popBatchIfComplete() {
    if (_chosenBatch.length != kBanknoteOcrBatchSize) return;
    Navigator.of(context).pop(List<String>.from(_chosenBatch));
  }

  @override
  void dispose() {
    _ocrTimer?.cancel();
    _ocrTimer = null;
    _manual.dispose();
    final c = _cam;
    final r = _recognizer;
    _cam = null;
    _recognizer = null;
    c?.dispose();
    r?.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Camera & nhận diện seri'),
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => Navigator.of(context).pop(),
        ),
        actions: [
          IconButton(
            tooltip: 'Một ảnh từ thư viện',
            onPressed: _pickGalleryOnce,
            icon: const Icon(Icons.photo_library_outlined),
          ),
        ],
      ),
      body: _initError != null
          ? Center(child: Padding(padding: const EdgeInsets.all(24), child: Text(_initError!)))
          : !_ready || _cam == null
              ? const Center(child: CircularProgressIndicator())
              : Column(
                  children: [
                    Expanded(
                      flex: 11,
                      child: ColoredBox(
                        color: Colors.black,
                        child: Center(child: CameraPreview(_cam!)),
                      ),
                    ),
                    const Divider(height: 1),
                    Expanded(
                      flex: 9,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Material(
                            color: Theme.of(context).colorScheme.surfaceContainerHighest.withOpacity(0.35),
                            child: Padding(
                              padding: const EdgeInsets.fromLTRB(12, 8, 12, 8),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: [
                                  Text(
                                    'Tập hiện tại: ${_chosenBatch.length}/$kBanknoteOcrBatchSize seri',
                                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                                          fontWeight: FontWeight.w700,
                                        ),
                                  ),
                                  const SizedBox(height: 6),
                                  if (_chosenBatch.isEmpty)
                                    Text(
                                      'Chạm ứng viên bên dưới để chọn/bỏ chọn (tối đa $kBanknoteOcrBatchSize).',
                                      style: TextStyle(fontSize: 12, color: Colors.grey.shade700),
                                    )
                                  else
                                    Wrap(
                                      spacing: 6,
                                      runSpacing: 6,
                                      children: [
                                        for (final s in _chosenBatch)
                                          InputChip(
                                            label: Text(s, style: const TextStyle(fontFamily: 'monospace', fontSize: 12)),
                                            onDeleted: () => setState(() => _chosenBatch.remove(s)),
                                            materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                                            visualDensity: VisualDensity.compact,
                                          ),
                                      ],
                                    ),
                                  const SizedBox(height: 8),
                                  FilledButton.icon(
                                    onPressed: _chosenBatch.length == kBanknoteOcrBatchSize ? _popBatchIfComplete : null,
                                    icon: const Icon(Icons.save_outlined),
                                    label: const Text('Lưu tập và thoát'),
                                  ),
                                ],
                              ),
                            ),
                          ),
                          Padding(
                            padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
                            child: Row(
                              children: [
                                Text(
                                  'Ứng viên (${_candidates.length})',
                                  style: Theme.of(context).textTheme.titleSmall,
                                ),
                                const Spacer(),
                                if (_candidates.isNotEmpty)
                                  TextButton(
                                    onPressed: () => setState(() => _candidateSet.clear()),
                                    child: const Text('Xóa ứng viên'),
                                  ),
                              ],
                            ),
                          ),
                          Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 12),
                            child: Text(
                              'Khoảng 1,2 giây chụp một khung để đọc. Chạm dòng để thêm/bỏ trong tập — cần đủ $kBanknoteOcrBatchSize seri rồi bấm Lưu tập.',
                              style: TextStyle(fontSize: 12, color: Colors.grey.shade700),
                            ),
                          ),
                          Expanded(
                            child: _candidates.isEmpty
                                ? Center(
                                    child: Text(
                                      'Chưa nhận diện được seri.\nĐủ sáng, lấy nét; hoặc dùng ảnh thư viện (icon góc).',
                                      textAlign: TextAlign.center,
                                      style: TextStyle(color: Colors.grey.shade600),
                                    ),
                                  )
                                : ListView.builder(
                                    padding: const EdgeInsets.only(bottom: 8),
                                    itemCount: _candidates.length,
                                    itemBuilder: (_, i) {
                                      final c = _candidates[i];
                                      final norm = normalizeBanknoteSerialStored(c);
                                      final inBatch = norm != null && _chosenBatch.contains(norm);
                                      return ListTile(
                                        dense: true,
                                        leading: Icon(
                                          inBatch ? Icons.check_circle : Icons.radio_button_unchecked,
                                          size: 22,
                                          color: inBatch ? Theme.of(context).colorScheme.primary : null,
                                        ),
                                        title: Text(
                                          c,
                                          style: const TextStyle(
                                            fontFamily: 'monospace',
                                            fontWeight: FontWeight.w600,
                                            letterSpacing: 0.5,
                                          ),
                                        ),
                                        subtitle: Text(
                                          inBatch ? 'Đang trong tập — chạm để bỏ' : 'Chạm để thêm vào tập',
                                          style: TextStyle(fontSize: 11, color: Colors.grey.shade600),
                                        ),
                                        onTap: () => _toggleCandidateIntoBatch(c),
                                      );
                                    },
                                  ),
                          ),
                          Padding(
                            padding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                Expanded(
                                  child: TextField(
                                    controller: _manual,
                                    decoration: const InputDecoration(
                                      labelText: 'Nhập seri bổ sung',
                                      border: OutlineInputBorder(),
                                      isDense: true,
                                    ),
                                    textCapitalization: TextCapitalization.characters,
                                    onSubmitted: (_) => _addManualToBatch(),
                                  ),
                                ),
                                const SizedBox(width: 8),
                                FilledButton.tonal(
                                  onPressed: _addManualToBatch,
                                  child: const Text('Thêm vào tập'),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
    );
  }
}

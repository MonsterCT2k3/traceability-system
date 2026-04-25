/// Số seri trong một tập khi hoàn tất từ màn camera + OCR.
const int kBanknoteOcrBatchSize = 5;

/// Chuẩn hóa seri lưu trữ (nhập tay / sau khi chọn từ OCR).
bool isValidBanknoteSerialStored(String s) {
  final t = s.trim();
  if (t.length < 4 || t.length > 32) return false;
  return RegExp(r'^[A-Za-z0-9\-]+$').hasMatch(t);
}

/// Trả về seri in hoa nếu hợp lệ, ngược lại `null`.
String? normalizeBanknoteSerialStored(String s) {
  final t = s.trim();
  if (!isValidBanknoteSerialStored(t)) return null;
  return t.toUpperCase();
}

/// Tách các chuỗi giống số seri in trên tờ tiền polymer (thường chữ + số).
/// OCR hay lệch khoảng trắng / nhầm O↔0 — người dùng vẫn xác nhận ở bước sau.
List<String> parseBanknoteSerialCandidates(String recognizedText) {
  final u = recognizedText.toUpperCase();
  final out = <String>{};

  final compact = u.replaceAll(RegExp(r'[\s\n\r]+'), '');

  // Hai chữ + 7–11 chữ số (tờ 1000đ polymer — dạng thường gặp)
  for (final m in RegExp(r'[A-Z]{2}\d{7,11}').allMatches(compact)) {
    final s = m.group(0)!;
    if (s.length <= 32) out.add(s);
  }

  // Một chữ + dài hơn (một số mẫu note)
  for (final m in RegExp(r'[A-Z]\d{8,11}').allMatches(compact)) {
    final s = m.group(0)!;
    if (s.length >= 9 && s.length <= 32) out.add(s);
  }

  // Dòng có khoảng: "AB 12345678"
  final loose = u.replaceAll(RegExp(r'\s+'), ' ');
  for (final m in RegExp(r'\b([A-Z]{1,2})\s+(\d{7,10})\b').allMatches(loose)) {
    final merged = '${m.group(1)}${m.group(2)}';
    if (merged.length <= 32) out.add(merged);
  }

  final list = out.toList()..sort();
  return list;
}

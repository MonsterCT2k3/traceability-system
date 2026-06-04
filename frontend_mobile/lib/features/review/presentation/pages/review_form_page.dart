import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';
import 'package:image_picker/image_picker.dart';

import '../../../../core/network/api_client.dart';
import '../../data/review_models.dart';

class ReviewFormPage extends StatefulWidget {
  final ClaimProduct product;
  final String claimToken;
  const ReviewFormPage(
      {super.key, required this.product, required this.claimToken});
  @override
  State<ReviewFormPage> createState() => _ReviewFormPageState();
}

class _ReviewFormPageState extends State<ReviewFormPage> {
  final content = TextEditingController();
  final picker = ImagePicker();
  final images = <XFile>[];
  XFile? video;
  int rating = 0;
  bool submitting = false;
  double progress = 0;

  bool get editing => widget.product.existingReview != null;

  @override
  void initState() {
    super.initState();
    final existing = widget.product.existingReview;
    if (existing != null) {
      rating = existing.rating;
      content.text = existing.content ?? '';
    }
  }

  @override
  void dispose() {
    content.dispose();
    super.dispose();
  }

  Future<void> addImages() async {
    final selected =
        await picker.pickMultiImage(imageQuality: 82, limit: 5 - images.length);
    if (!mounted) return;
    setState(() => images.addAll(selected.take(5 - images.length)));
  }

  Future<void> addVideo() async {
    final selected = await picker.pickVideo(
        source: ImageSource.gallery, maxDuration: const Duration(seconds: 30));
    if (selected != null && mounted) setState(() => video = selected);
  }

  Future<String> upload(XFile file, String type) async {
    final form = FormData.fromMap({
      'mediaType': type,
      'file': await MultipartFile.fromFile(file.path, filename: file.name),
    });
    final response = await GetIt.I<ApiClient>()
        .post('/product/api/v1/review-media', data: form);
    return response.data['result']['id'].toString();
  }

  Future<void> submit() async {
    if (rating == 0) {
      ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Hãy chọn số sao trước khi gửi')));
      return;
    }
    setState(() {
      submitting = true;
      progress = 0;
    });
    try {
      final files = <(XFile, String)>[
        ...images.map((e) => (e, 'IMAGE')),
        if (video != null) (video!, 'VIDEO')
      ];
      final mediaIds = <String>[];
      for (var i = 0; i < files.length; i++) {
        mediaIds.add(await upload(files[i].$1, files[i].$2));
        if (mounted) setState(() => progress = (i + 1) / (files.length + 1));
      }
      final data = {
        'claimToken': widget.claimToken,
        'rating': rating,
        'content': content.text.trim(),
        'mediaIds': mediaIds,
      };
      if (editing) {
        await GetIt.I<ApiClient>().put(
          '/product/api/v1/reviews/${widget.product.existingReview!.id}',
          data: data,
        );
      } else {
        await GetIt.I<ApiClient>().post('/product/api/v1/reviews', data: data);
      }
      if (!mounted) return;
      setState(() => progress = 1);
      await showDialog(
          context: context, builder: (_) => const _SuccessDialog());
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Không thể gửi đánh giá: $e')));
      }
    } finally {
      if (mounted) setState(() => submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    const green = Color(0xFF147B5D);
    return Scaffold(
      backgroundColor: const Color(0xFFF3F7F4),
      appBar: AppBar(title: const Text('Đánh giá sản phẩm')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 32),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
                color: const Color(0xFF153E34),
                borderRadius: BorderRadius.circular(8)),
            child: Row(children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(6),
                child: widget.product.productImageUrl?.isNotEmpty == true
                    ? Image.network(widget.product.productImageUrl!,
                        width: 72, height: 72, fit: BoxFit.cover)
                    : Container(
                        width: 72,
                        height: 72,
                        color: Colors.white12,
                        child: const Icon(Icons.inventory_2_outlined,
                            color: Colors.white)),
              ),
              const SizedBox(width: 14),
              Expanded(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    Text(widget.product.productName,
                        style: const TextStyle(
                            color: Colors.white,
                            fontSize: 17,
                            fontWeight: FontWeight.w800)),
                    const SizedBox(height: 5),
                    Text(widget.product.unitSerial,
                        style: const TextStyle(color: Colors.white60)),
                    const SizedBox(height: 9),
                    const Row(children: [
                      Icon(Icons.verified_user_rounded,
                          size: 16, color: Color(0xFF82E0B6)),
                      SizedBox(width: 5),
                      Text('Đã xác minh sở hữu sản phẩm',
                          style: TextStyle(
                              color: Color(0xFF82E0B6),
                              fontSize: 12,
                              fontWeight: FontWeight.w700))
                    ]),
                  ])),
            ]),
          ),
          const SizedBox(height: 22),
          const Text('Trải nghiệm của bạn',
              style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w800,
                  color: Color(0xFF17352D))),
          const SizedBox(height: 12),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(vertical: 18),
            decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: const Color(0xFFDCE9E2))),
            child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(
                    5,
                    (index) => IconButton(
                          tooltip: '${index + 1} sao',
                          onPressed: () => setState(() => rating = index + 1),
                          icon: Icon(
                              index < rating
                                  ? Icons.star_rounded
                                  : Icons.star_border_rounded,
                              size: 38,
                              color: const Color(0xFFFFB020)),
                        ))),
          ),
          const SizedBox(height: 14),
          TextField(
            controller: content,
            minLines: 4,
            maxLines: 7,
            maxLength: 2000,
            decoration: InputDecoration(
              hintText:
                  'Chia sẻ cảm nhận về chất lượng, hương vị hoặc bao bì...',
              filled: true,
              fillColor: Colors.white,
              border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: const BorderSide(color: Color(0xFFDCE9E2))),
              enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: const BorderSide(color: Color(0xFFDCE9E2))),
            ),
          ),
          const SizedBox(height: 8),
          if (editing) ...[
            const Text(
              'Ảnh và video đã đăng được giữ nguyên khi chỉnh sửa.',
              style: TextStyle(color: Color(0xFF60736D), fontSize: 12),
            ),
            if (widget.product.existingReview!.media.isNotEmpty) ...[
              const SizedBox(height: 10),
              SizedBox(
                height: 82,
                child: ListView(
                  scrollDirection: Axis.horizontal,
                  children: widget.product.existingReview!.media
                      .map((media) => Container(
                            width: 82,
                            margin: const EdgeInsets.only(right: 8),
                            clipBehavior: Clip.antiAlias,
                            decoration: BoxDecoration(
                              color: Colors.black12,
                              borderRadius: BorderRadius.circular(6),
                            ),
                            child: Stack(fit: StackFit.expand, children: [
                              Image.network(
                                  media.thumbnailUrl ?? media.mediaUrl,
                                  fit: BoxFit.cover),
                              if (media.mediaType == 'VIDEO')
                                const Icon(Icons.play_circle_fill_rounded,
                                    color: Colors.white, size: 34),
                            ]),
                          ))
                      .toList(),
                ),
              ),
            ],
          ] else ...[
            Row(children: [
              Expanded(
                  child: OutlinedButton.icon(
                      onPressed:
                          images.length < 5 && !submitting ? addImages : null,
                      icon: const Icon(Icons.add_photo_alternate_outlined),
                      label: Text('Ảnh ${images.length}/5'))),
              const SizedBox(width: 10),
              Expanded(
                  child: OutlinedButton.icon(
                      onPressed: video == null && !submitting ? addVideo : null,
                      icon: const Icon(Icons.video_library_outlined),
                      label: Text(
                          video == null ? 'Thêm video' : 'Đã chọn video'))),
            ]),
          ],
          if (!editing && (images.isNotEmpty || video != null)) ...[
            const SizedBox(height: 14),
            SizedBox(
                height: 82,
                child: ListView(scrollDirection: Axis.horizontal, children: [
                  ...images.asMap().entries.map((e) => _MediaChip(
                      label: e.value.name,
                      icon: Icons.image_outlined,
                      onRemove: () => setState(() => images.removeAt(e.key)))),
                  if (video != null)
                    _MediaChip(
                        label: video!.name,
                        icon: Icons.play_circle_outline,
                        onRemove: () => setState(() => video = null)),
                ])),
          ],
          if (submitting) ...[
            const SizedBox(height: 18),
            LinearProgressIndicator(value: progress, color: green),
            const SizedBox(height: 7),
            const Text('Đang tải media và gửi đánh giá...',
                style: TextStyle(color: green, fontWeight: FontWeight.w600)),
          ],
          const SizedBox(height: 22),
          SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                onPressed: submitting ? null : submit,
                style: FilledButton.styleFrom(
                    backgroundColor: green,
                    padding: const EdgeInsets.symmetric(vertical: 15)),
                icon: const Icon(Icons.send_rounded),
                label: Text(editing ? 'Lưu thay đổi' : 'Gửi đánh giá',
                    style: const TextStyle(fontWeight: FontWeight.w700)),
              )),
        ]),
      ),
    );
  }
}

class _MediaChip extends StatelessWidget {
  final String label;
  final IconData icon;
  final VoidCallback onRemove;
  const _MediaChip(
      {required this.label, required this.icon, required this.onRemove});
  @override
  Widget build(BuildContext context) => Container(
        width: 120,
        margin: const EdgeInsets.only(right: 9),
        padding: const EdgeInsets.all(9),
        decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: const Color(0xFFDCE9E2))),
        child: Column(children: [
          Row(children: [
            Icon(icon, color: const Color(0xFF147B5D)),
            const Spacer(),
            InkWell(onTap: onRemove, child: const Icon(Icons.close, size: 18))
          ]),
          const Spacer(),
          Text(label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 11))
        ]),
      );
}

class _SuccessDialog extends StatelessWidget {
  const _SuccessDialog();
  @override
  Widget build(BuildContext context) => AlertDialog(
        icon: const Icon(Icons.verified_rounded,
            color: Color(0xFF147B5D), size: 48),
        title: const Text('Đã gửi đánh giá'),
        content: const Text('Cảm ơn bạn đã chia sẻ trải nghiệm thực tế.'),
        actions: [
          FilledButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Hoàn tất'))
        ],
      );
}

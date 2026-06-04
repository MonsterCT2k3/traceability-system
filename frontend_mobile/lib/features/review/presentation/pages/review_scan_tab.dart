import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../../../../core/network/api_client.dart';
import '../../data/review_models.dart';
import 'review_form_page.dart';

class ReviewScanTab extends StatefulWidget {
  final bool showBackButton;
  const ReviewScanTab({super.key, this.showBackButton = false});
  @override
  State<ReviewScanTab> createState() => _ReviewScanTabState();
}

class _ReviewScanTabState extends State<ReviewScanTab> {
  final scanner =
      MobileScannerController(detectionSpeed: DetectionSpeed.noDuplicates);
  bool busy = false;

  Future<void> resolve(String? raw) async {
    if (busy || raw == null || !raw.trim().toUpperCase().startsWith('CLM-')) {
      return;
    }
    setState(() => busy = true);
    await scanner.stop();
    try {
      final response = await GetIt.I<ApiClient>().post(
          '/product/api/v1/claims/resolve',
          data: {'claimToken': raw.trim()});
      final product = ClaimProduct.fromJson(
          Map<String, dynamic>.from(response.data['result'] as Map));
      if (!mounted) return;
      if (product.claimStatus == 'CLAIMED_BY_ME') {
        final edit = await showDialog<bool>(
          context: context,
          builder: (_) => _ExistingReviewDialog(product: product),
        );
        if (edit == true && mounted) {
          await Navigator.push(
              context,
              MaterialPageRoute(
                  builder: (_) => ReviewFormPage(
                      product: product, claimToken: raw.trim())));
        }
      } else {
        final created = await Navigator.push<bool>(
            context,
            MaterialPageRoute(
                builder: (_) =>
                    ReviewFormPage(product: product, claimToken: raw.trim())));
        if (created == true && widget.showBackButton && mounted) {
          Navigator.pop(context, true);
          return;
        }
      }
    } on DioException catch (e) {
      if (mounted) {
        _message(e.response?.data is Map
            ? '${e.response?.data['message']}'
            : 'Claim QR không hợp lệ');
      }
    } catch (e) {
      if (mounted) _message('$e');
    } finally {
      if (mounted) {
        setState(() => busy = false);
        await scanner.start();
      }
    }
  }

  void _message(String text) =>
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));

  @override
  void dispose() {
    scanner.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF081E1A),
      body: Stack(children: [
        MobileScanner(
            controller: scanner,
            onDetect: (capture) =>
                resolve(capture.barcodes.firstOrNull?.rawValue)),
        Container(color: Colors.black26),
        SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 18, 20, 125),
            child: Column(children: [
              Row(children: [
                if (widget.showBackButton) ...[
                  IconButton.filledTonal(
                    onPressed: () => Navigator.pop(context),
                    icon: const Icon(Icons.arrow_back_rounded),
                  ),
                  const SizedBox(width: 8),
                ] else ...[
                  const Icon(Icons.rate_review_rounded, color: Colors.white),
                  const SizedBox(width: 10),
                ],
                const Text('Đánh giá sản phẩm',
                    style: TextStyle(
                        color: Colors.white,
                        fontSize: 20,
                        fontWeight: FontWeight.w800)),
              ]),
              const Spacer(),
              Container(
                width: 270,
                height: 270,
                decoration: BoxDecoration(
                  border: Border.all(color: const Color(0xFF73E3B2), width: 3),
                  borderRadius: BorderRadius.circular(18),
                ),
              ),
              const SizedBox(height: 26),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
                decoration: BoxDecoration(
                    color: Colors.black54,
                    borderRadius: BorderRadius.circular(8)),
                child: const Text('Cào lớp phủ và đưa QR đánh giá vào khung',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                        color: Colors.white, fontWeight: FontWeight.w600)),
              ),
              const Spacer(),
            ]),
          ),
        ),
        if (busy)
          Container(
              color: Colors.black54,
              child: const Center(
                  child: Column(mainAxisSize: MainAxisSize.min, children: [
                CircularProgressIndicator(color: Color(0xFF73E3B2)),
                SizedBox(height: 14),
                Text('Đang kiểm tra Claim QR...',
                    style: TextStyle(
                        color: Colors.white, fontWeight: FontWeight.w600))
              ]))),
      ]),
    );
  }
}

class _ExistingReviewDialog extends StatelessWidget {
  final ClaimProduct product;
  const _ExistingReviewDialog({required this.product});

  @override
  Widget build(BuildContext context) {
    final review = product.existingReview;
    return AlertDialog(
      icon: const Icon(Icons.verified_rounded,
          color: Color(0xFF147B5D), size: 42),
      title: const Text('Bạn đã đánh giá sản phẩm này'),
      content: Column(mainAxisSize: MainAxisSize.min, children: [
        Text(product.productName,
            textAlign: TextAlign.center,
            style: const TextStyle(fontWeight: FontWeight.w700)),
        const SizedBox(height: 12),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: List.generate(
            5,
            (index) => Icon(
              index < (review?.rating ?? 0)
                  ? Icons.star_rounded
                  : Icons.star_border_rounded,
              color: const Color(0xFFFFB020),
            ),
          ),
        ),
        if (review?.content?.isNotEmpty == true) ...[
          const SizedBox(height: 12),
          Text(review!.content!, textAlign: TextAlign.center),
        ],
      ]),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context, true),
          child: const Text('Chỉnh sửa'),
        ),
        FilledButton(
          onPressed: () => Navigator.pop(context, false),
          child: const Text('Đã hiểu'),
        ),
      ],
    );
  }
}

import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';

import '../../../../core/network/api_client.dart';
import '../../data/review_models.dart';
import 'review_form_page.dart';

class PurchasedProductDetailPage extends StatefulWidget {
  final ProductReviewModel myReview;

  const PurchasedProductDetailPage({super.key, required this.myReview});

  @override
  State<PurchasedProductDetailPage> createState() =>
      _PurchasedProductDetailPageState();
}

class _PurchasedProductDetailPageState
    extends State<PurchasedProductDetailPage> {
  late ProductReviewModel myReview;
  late Future<List<ProductReviewModel>> reviews;

  @override
  void initState() {
    super.initState();
    myReview = widget.myReview;
    reviews = loadReviews();
  }

  Future<List<ProductReviewModel>> loadReviews() async {
    final response = await GetIt.I<ApiClient>().get(
      '/product/api/v1/products/${myReview.productId}/reviews',
      queryParameters: {'size': 50},
    );
    final page = Map<String, dynamic>.from(response.data['result'] as Map);
    return (page['content'] as List<dynamic>? ?? const [])
        .map((item) =>
            ProductReviewModel.fromJson(Map<String, dynamic>.from(item as Map)))
        .toList();
  }

  Future<void> editMyReview() async {
    final product = ClaimProduct(
      claimStatus: 'CLAIMED_BY_ME',
      productUnitId: '',
      unitSerial: myReview.unitSerial,
      productId: myReview.productId,
      productName: myReview.productName,
      productImageUrl: myReview.productImageUrl,
      existingReview: myReview,
    );
    final updated = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (_) => ReviewFormPage(product: product, claimToken: ''),
      ),
    );
    if (updated == true && mounted) {
      final mine = await GetIt.I<ApiClient>().get(
        '/product/api/v1/reviews/my',
        queryParameters: {'size': 50},
      );
      final page = Map<String, dynamic>.from(mine.data['result'] as Map);
      final items = (page['content'] as List<dynamic>? ?? const [])
          .map((item) => ProductReviewModel.fromJson(
              Map<String, dynamic>.from(item as Map)))
          .toList();
      setState(() {
        myReview = items.firstWhere((item) => item.id == myReview.id);
        reviews = loadReviews();
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF3F7F4),
      appBar: AppBar(title: const Text('Chi tiết sản phẩm')),
      body: RefreshIndicator(
        onRefresh: () async => setState(() => reviews = loadReviews()),
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 32),
          children: [
            _ProductHero(review: myReview),
            const SizedBox(height: 18),
            Row(children: [
              const Expanded(
                child: Text('Đánh giá sản phẩm',
                    style:
                        TextStyle(fontSize: 19, fontWeight: FontWeight.w800)),
              ),
              OutlinedButton.icon(
                onPressed: editMyReview,
                icon: const Icon(Icons.edit_outlined, size: 18),
                label: const Text('Sửa đánh giá'),
              ),
            ]),
            const SizedBox(height: 10),
            FutureBuilder<List<ProductReviewModel>>(
              future: reviews,
              builder: (context, snapshot) {
                if (snapshot.connectionState != ConnectionState.done) {
                  return const Padding(
                    padding: EdgeInsets.all(30),
                    child: Center(child: CircularProgressIndicator()),
                  );
                }
                if (snapshot.hasError) {
                  return const Padding(
                    padding: EdgeInsets.all(24),
                    child: Text('Không tải được danh sách đánh giá.',
                        textAlign: TextAlign.center),
                  );
                }
                return Column(
                  children: (snapshot.data ?? const [])
                      .map((review) => _ReviewTile(
                            review: review,
                            mine: review.id == myReview.id,
                            onEdit: editMyReview,
                          ))
                      .toList(),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _ProductHero extends StatelessWidget {
  final ProductReviewModel review;
  const _ProductHero({required this.review});

  @override
  Widget build(BuildContext context) {
    return Container(
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: const Color(0xFF153E34),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        if (review.productImageUrl?.isNotEmpty == true)
          Image.network(review.productImageUrl!,
              width: double.infinity, height: 190, fit: BoxFit.cover),
        Padding(
          padding: const EdgeInsets.all(17),
          child:
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(review.productName,
                style: const TextStyle(
                    color: Colors.white,
                    fontSize: 21,
                    fontWeight: FontWeight.w800)),
            const SizedBox(height: 5),
            Text(review.unitSerial,
                style: const TextStyle(color: Colors.white60)),
            if (review.productDescription?.trim().isNotEmpty == true) ...[
              const SizedBox(height: 10),
              Text(review.productDescription!,
                  style: const TextStyle(color: Colors.white70, height: 1.4)),
            ],
            const SizedBox(height: 13),
            const Row(children: [
              Icon(Icons.verified_rounded, color: Color(0xFF82E0B6), size: 18),
              SizedBox(width: 6),
              Text('Bạn đã xác minh sở hữu sản phẩm này',
                  style: TextStyle(
                      color: Color(0xFF82E0B6), fontWeight: FontWeight.w700)),
            ]),
          ]),
        ),
      ]),
    );
  }
}

class _ReviewTile extends StatelessWidget {
  final ProductReviewModel review;
  final bool mine;
  final VoidCallback onEdit;

  const _ReviewTile(
      {required this.review, required this.mine, required this.onEdit});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: mine ? const Color(0xFFE7F5EE) : Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
            color: mine ? const Color(0xFF72B99C) : const Color(0xFFDCE9E2)),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          CircleAvatar(
            foregroundImage: review.reviewerAvatarUrl?.isNotEmpty == true
                ? NetworkImage(review.reviewerAvatarUrl!)
                : null,
            child: Text(review.reviewerName.isEmpty
                ? '?'
                : review.reviewerName[0].toUpperCase()),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(mine ? 'Đánh giá của bạn' : review.reviewerName,
                    style: const TextStyle(fontWeight: FontWeight.w800)),
                const Text('Đã xác minh sở hữu',
                    style: TextStyle(fontSize: 11, color: Color(0xFF147B5D))),
              ],
            ),
          ),
          if (mine)
            IconButton(
              tooltip: 'Chỉnh sửa đánh giá',
              onPressed: onEdit,
              icon: const Icon(Icons.edit_outlined),
            ),
        ]),
        const SizedBox(height: 9),
        Row(
          children: List.generate(
            5,
            (index) => Icon(
              index < review.rating
                  ? Icons.star_rounded
                  : Icons.star_border_rounded,
              size: 19,
              color: const Color(0xFFFFA000),
            ),
          ),
        ),
        if (review.content?.isNotEmpty == true) ...[
          const SizedBox(height: 9),
          Text(review.content!, style: const TextStyle(height: 1.4)),
        ],
        if (review.media.isNotEmpty) ...[
          const SizedBox(height: 12),
          SizedBox(
            height: 82,
            child: ListView(
              scrollDirection: Axis.horizontal,
              children: review.media
                  .map((media) => Container(
                        width: 82,
                        margin: const EdgeInsets.only(right: 8),
                        clipBehavior: Clip.antiAlias,
                        decoration: BoxDecoration(
                          color: Colors.black12,
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Stack(fit: StackFit.expand, children: [
                          Image.network(media.thumbnailUrl ?? media.mediaUrl,
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
      ]),
    );
  }
}

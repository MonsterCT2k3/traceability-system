import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';

import '../../../../core/network/api_client.dart';
import '../../data/review_models.dart';

class ProductReviewsSummary extends StatefulWidget {
  final String productId;

  const ProductReviewsSummary({super.key, required this.productId});

  @override
  State<ProductReviewsSummary> createState() => _ProductReviewsSummaryState();
}

class _ProductReviewsSummaryState extends State<ProductReviewsSummary> {
  late Future<({double average, int total})> future;

  @override
  void initState() {
    super.initState();
    future = load();
  }

  Future<({double average, int total})> load() async {
    final response = await GetIt.I<ApiClient>()
        .get('/product/api/v1/products/${widget.productId}/review-summary');
    final result = Map<String, dynamic>.from(response.data['result'] as Map);
    return (
      average: double.tryParse('${result['averageRating']}') ?? 0,
      total: int.tryParse('${result['totalReviews']}') ?? 0,
    );
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<({double average, int total})>(
      future: future,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const _SummaryShell(
            child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
          );
        }
        if (snapshot.hasError) return const SizedBox.shrink();
        final data = snapshot.data!;
        return Material(
          color: Colors.white,
          borderRadius: BorderRadius.circular(8),
          child: InkWell(
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute(
                builder: (_) => ProductReviewsPage(
                  productId: widget.productId,
                  initialAverage: data.average,
                  initialTotal: data.total,
                ),
              ),
            ),
            borderRadius: BorderRadius.circular(8),
            child: _SummaryShell(
              child: Row(children: [
                Container(
                  width: 50,
                  height: 50,
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFF4D7),
                    borderRadius: BorderRadius.circular(7),
                  ),
                  child: const Icon(Icons.star_rounded,
                      color: Color(0xFFFFA000), size: 30),
                ),
                const SizedBox(width: 13),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Đánh giá sản phẩm',
                          style: TextStyle(
                              fontSize: 16, fontWeight: FontWeight.w800)),
                      const SizedBox(height: 4),
                      Text(
                        data.total == 0
                            ? 'Chưa có đánh giá'
                            : '${data.average.toStringAsFixed(1)}/5 · ${data.total} đánh giá',
                        style: const TextStyle(color: Color(0xFF60736D)),
                      ),
                    ],
                  ),
                ),
                const Icon(Icons.chevron_right_rounded,
                    color: Color(0xFF7B8D86)),
              ]),
            ),
          ),
        );
      },
    );
  }
}

class _SummaryShell extends StatelessWidget {
  final Widget child;
  const _SummaryShell({required this.child});

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minHeight: 78),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFDCE9E2)),
      ),
      child: child,
    );
  }
}

class ProductReviewsPage extends StatefulWidget {
  final String productId;
  final double initialAverage;
  final int initialTotal;

  const ProductReviewsPage({
    super.key,
    required this.productId,
    required this.initialAverage,
    required this.initialTotal,
  });

  @override
  State<ProductReviewsPage> createState() => _ProductReviewsPageState();
}

class _ProductReviewsPageState extends State<ProductReviewsPage> {
  late Future<List<ProductReviewModel>> future;

  @override
  void initState() {
    super.initState();
    future = load();
  }

  Future<List<ProductReviewModel>> load() async {
    final response = await GetIt.I<ApiClient>().get(
      '/product/api/v1/products/${widget.productId}/reviews',
      queryParameters: {'size': 50},
    );
    final page = Map<String, dynamic>.from(response.data['result'] as Map);
    return (page['content'] as List<dynamic>? ?? const [])
        .map((item) =>
            ProductReviewModel.fromJson(Map<String, dynamic>.from(item as Map)))
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF3F7F4),
      appBar: AppBar(title: const Text('Đánh giá sản phẩm')),
      body: RefreshIndicator(
        onRefresh: () async => setState(() => future = load()),
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 14, 16, 32),
          children: [
            Container(
              padding: const EdgeInsets.all(18),
              decoration: BoxDecoration(
                color: const Color(0xFF153E34),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(children: [
                Text(widget.initialAverage.toStringAsFixed(1),
                    style: const TextStyle(
                        color: Colors.white,
                        fontSize: 40,
                        fontWeight: FontWeight.w800)),
                const SizedBox(width: 13),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: List.generate(
                          5,
                          (index) => Icon(
                            index < widget.initialAverage.round()
                                ? Icons.star_rounded
                                : Icons.star_border_rounded,
                            color: const Color(0xFFFFC44D),
                            size: 22,
                          ),
                        ),
                      ),
                      const SizedBox(height: 5),
                      Text('${widget.initialTotal} đánh giá đã xác minh sở hữu',
                          style: const TextStyle(color: Colors.white70)),
                    ],
                  ),
                ),
              ]),
            ),
            const SizedBox(height: 16),
            FutureBuilder<List<ProductReviewModel>>(
              future: future,
              builder: (context, snapshot) {
                if (snapshot.connectionState != ConnectionState.done) {
                  return const Padding(
                    padding: EdgeInsets.all(32),
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
                final reviews = snapshot.data ?? const [];
                if (reviews.isEmpty) {
                  return const Padding(
                    padding: EdgeInsets.all(30),
                    child: Text('Chưa có đánh giá cho sản phẩm này.',
                        textAlign: TextAlign.center),
                  );
                }
                return Column(
                  children: reviews
                      .map((review) => _ReviewCard(review: review))
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

class _ReviewCard extends StatelessWidget {
  final ProductReviewModel review;
  const _ReviewCard({required this.review});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFDCE9E2)),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          CircleAvatar(
            radius: 20,
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
                Text(review.reviewerName,
                    style: const TextStyle(fontWeight: FontWeight.w700)),
                const Row(children: [
                  Icon(Icons.verified_user_rounded,
                      size: 13, color: Color(0xFF147B5D)),
                  SizedBox(width: 4),
                  Text('Đã xác minh sở hữu',
                      style: TextStyle(
                          fontSize: 10,
                          color: Color(0xFF147B5D),
                          fontWeight: FontWeight.w700)),
                ]),
              ],
            ),
          ),
          Row(
            children: List.generate(
              review.rating,
              (_) => const Icon(Icons.star_rounded,
                  color: Color(0xFFFFA000), size: 17),
            ),
          ),
        ]),
        if (review.content?.isNotEmpty == true) ...[
          const SizedBox(height: 11),
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

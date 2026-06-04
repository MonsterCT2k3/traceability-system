import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';

import '../../../../core/network/api_client.dart';
import '../../data/review_models.dart';
import 'purchased_product_detail_page.dart';
import 'review_scan_tab.dart';

class PurchasedProductsTab extends StatefulWidget {
  const PurchasedProductsTab({super.key});

  @override
  State<PurchasedProductsTab> createState() => _PurchasedProductsTabState();
}

class _PurchasedProductsTabState extends State<PurchasedProductsTab> {
  late Future<List<ProductReviewModel>> future;

  @override
  void initState() {
    super.initState();
    future = load();
  }

  Future<List<ProductReviewModel>> load() async {
    final response = await GetIt.I<ApiClient>().get(
      '/product/api/v1/reviews/my',
      queryParameters: {'size': 50},
    );
    final page = Map<String, dynamic>.from(response.data['result'] as Map);
    return (page['content'] as List<dynamic>? ?? const [])
        .map((item) =>
            ProductReviewModel.fromJson(Map<String, dynamic>.from(item as Map)))
        .toList();
  }

  void refresh() => setState(() => future = load());

  Future<void> scanNewProduct() async {
    await Navigator.push(
      context,
      MaterialPageRoute(
          builder: (_) => const ReviewScanTab(showBackButton: true)),
    );
    if (mounted) refresh();
  }

  Future<void> openProduct(ProductReviewModel review) async {
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => PurchasedProductDetailPage(myReview: review),
      ),
    );
    if (mounted) refresh();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF3F7F4),
      body: SafeArea(
        bottom: false,
        child: RefreshIndicator(
          onRefresh: () async => refresh(),
          child: CustomScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            slivers: [
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(18, 20, 18, 12),
                sliver: SliverToBoxAdapter(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Sản phẩm đã mua',
                        style: TextStyle(
                          fontSize: 25,
                          fontWeight: FontWeight.w800,
                          color: Color(0xFF17352D),
                        ),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        'Các sản phẩm đã xác minh bằng Claim QR',
                        style: TextStyle(color: Color(0xFF60736D)),
                      ),
                      const SizedBox(height: 16),
                      SizedBox(
                        width: double.infinity,
                        child: FilledButton.icon(
                          onPressed: scanNewProduct,
                          style: FilledButton.styleFrom(
                            padding: const EdgeInsets.symmetric(vertical: 14),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(8),
                            ),
                          ),
                          icon: const Icon(Icons.qr_code_scanner_rounded),
                          label: const Text('Đánh giá sản phẩm mới',
                              style: TextStyle(fontWeight: FontWeight.w700)),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(18, 4, 18, 120),
                sliver: FutureBuilder<List<ProductReviewModel>>(
                  future: future,
                  builder: (context, snapshot) {
                    if (snapshot.connectionState != ConnectionState.done) {
                      return const SliverFillRemaining(
                        hasScrollBody: false,
                        child: Center(child: CircularProgressIndicator()),
                      );
                    }
                    if (snapshot.hasError) {
                      return SliverFillRemaining(
                        hasScrollBody: false,
                        child: _EmptyState(
                          icon: Icons.cloud_off_rounded,
                          title: 'Không tải được sản phẩm',
                          message: 'Kéo xuống để thử lại.',
                          onPressed: refresh,
                          buttonLabel: 'Thử lại',
                        ),
                      );
                    }
                    final reviews = snapshot.data ?? const [];
                    if (reviews.isEmpty) {
                      return SliverFillRemaining(
                        hasScrollBody: false,
                        child: _EmptyState(
                          icon: Icons.shopping_bag_outlined,
                          title: 'Chưa có sản phẩm đã mua',
                          message:
                              'Quét Claim QR được phủ cào trên sản phẩm để tạo đánh giá xác minh.',
                          onPressed: scanNewProduct,
                          buttonLabel: 'Đánh giá sản phẩm mới',
                        ),
                      );
                    }
                    return SliverList.separated(
                      itemCount: reviews.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 12),
                      itemBuilder: (_, index) => _PurchasedProductCard(
                        review: reviews[index],
                        onTap: () => openProduct(reviews[index]),
                      ),
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PurchasedProductCard extends StatelessWidget {
  final ProductReviewModel review;
  final VoidCallback onTap;

  const _PurchasedProductCard({required this.review, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(8),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Padding(
          padding: const EdgeInsets.all(13),
          child: Row(children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(7),
              child: review.productImageUrl?.isNotEmpty == true
                  ? Image.network(review.productImageUrl!,
                      width: 78, height: 78, fit: BoxFit.cover)
                  : Container(
                      width: 78,
                      height: 78,
                      color: const Color(0xFFE8F2ED),
                      child: const Icon(Icons.inventory_2_outlined,
                          color: Color(0xFF147B5D)),
                    ),
            ),
            const SizedBox(width: 13),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(review.productName,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                          fontSize: 16, fontWeight: FontWeight.w800)),
                  const SizedBox(height: 5),
                  Text(review.unitSerial,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                          color: Color(0xFF6B7E77), fontSize: 12)),
                  const SizedBox(height: 8),
                  Row(children: [
                    const Icon(Icons.verified_rounded,
                        size: 15, color: Color(0xFF147B5D)),
                    const SizedBox(width: 4),
                    const Text('Đã xác minh',
                        style: TextStyle(
                            color: Color(0xFF147B5D),
                            fontSize: 11,
                            fontWeight: FontWeight.w700)),
                    const Spacer(),
                    Text('${review.rating} sao',
                        style: const TextStyle(
                            color: Color(0xFFFFA000),
                            fontWeight: FontWeight.w800)),
                  ]),
                ],
              ),
            ),
            const SizedBox(width: 6),
            const Icon(Icons.chevron_right_rounded, color: Color(0xFF8B9B95)),
          ]),
        ),
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  final IconData icon;
  final String title;
  final String message;
  final VoidCallback onPressed;
  final String buttonLabel;

  const _EmptyState({
    required this.icon,
    required this.title,
    required this.message,
    required this.onPressed,
    required this.buttonLabel,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Icon(icon, size: 58, color: const Color(0xFF147B5D)),
          const SizedBox(height: 16),
          Text(title,
              textAlign: TextAlign.center,
              style:
                  const TextStyle(fontSize: 18, fontWeight: FontWeight.w800)),
          const SizedBox(height: 7),
          Text(message,
              textAlign: TextAlign.center,
              style: const TextStyle(color: Color(0xFF60736D), height: 1.4)),
          const SizedBox(height: 20),
          FilledButton.icon(
            onPressed: onPressed,
            icon: const Icon(Icons.qr_code_scanner_rounded),
            label: Text(buttonLabel),
          ),
        ]),
      ),
    );
  }
}

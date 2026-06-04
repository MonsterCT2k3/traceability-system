class ClaimProduct {
  final String claimStatus;
  final String productUnitId;
  final String unitSerial;
  final String productId;
  final String productName;
  final String? productImageUrl;
  final ProductReviewModel? existingReview;

  const ClaimProduct({
    required this.claimStatus,
    required this.productUnitId,
    required this.unitSerial,
    required this.productId,
    required this.productName,
    this.productImageUrl,
    this.existingReview,
  });

  factory ClaimProduct.fromJson(Map<String, dynamic> json) => ClaimProduct(
        claimStatus: json['claimStatus']?.toString() ?? '',
        productUnitId: json['productUnitId']?.toString() ?? '',
        unitSerial: json['unitSerial']?.toString() ?? '',
        productId: json['productId']?.toString() ?? '',
        productName: json['productName']?.toString() ?? 'Sản phẩm',
        productImageUrl: json['productImageUrl']?.toString(),
        existingReview: json['existingReview'] is Map
            ? ProductReviewModel.fromJson(
                Map<String, dynamic>.from(json['existingReview'] as Map))
            : null,
      );
}

class ReviewMediaModel {
  final String id;
  final String mediaType;
  final String mediaUrl;
  final String? thumbnailUrl;
  const ReviewMediaModel(
      this.id, this.mediaType, this.mediaUrl, this.thumbnailUrl);
  factory ReviewMediaModel.fromJson(Map<String, dynamic> json) =>
      ReviewMediaModel(
        json['id']?.toString() ?? '',
        json['mediaType']?.toString() ?? '',
        json['mediaUrl']?.toString() ?? '',
        json['thumbnailUrl']?.toString(),
      );
}

class ProductReviewModel {
  final String id;
  final String productId;
  final String productName;
  final String? productImageUrl;
  final String? productDescription;
  final String unitSerial;
  final int rating;
  final String? content;
  final String reviewerName;
  final String? reviewerAvatarUrl;
  final List<ReviewMediaModel> media;
  final String? createdAt;
  const ProductReviewModel({
    required this.id,
    required this.productId,
    required this.productName,
    this.productImageUrl,
    this.productDescription,
    required this.unitSerial,
    required this.rating,
    this.content,
    required this.reviewerName,
    this.reviewerAvatarUrl,
    this.media = const [],
    this.createdAt,
  });
  factory ProductReviewModel.fromJson(Map<String, dynamic> json) =>
      ProductReviewModel(
        id: json['id']?.toString() ?? '',
        productId: json['productId']?.toString() ?? '',
        productName: json['productName']?.toString() ?? 'Sản phẩm',
        productImageUrl: json['productImageUrl']?.toString(),
        productDescription: json['productDescription']?.toString(),
        unitSerial: json['unitSerial']?.toString() ?? '',
        rating: int.tryParse('${json['rating']}') ?? 0,
        content: json['content']?.toString(),
        reviewerName: json['reviewerName']?.toString() ?? 'Người dùng',
        reviewerAvatarUrl: json['reviewerAvatarUrl']?.toString(),
        media: (json['media'] as List<dynamic>? ?? const [])
            .map((e) =>
                ReviewMediaModel.fromJson(Map<String, dynamic>.from(e as Map)))
            .toList(),
        createdAt: json['createdAt']?.toString(),
      );
}

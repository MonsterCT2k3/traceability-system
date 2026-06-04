import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:frontend_mobile/features/main/domain/entities/trace_entity.dart';
import 'package:frontend_mobile/features/main/presentation/bloc/trace/trace_bloc.dart';
import 'package:frontend_mobile/features/main/presentation/bloc/trace/trace_state.dart';
import 'package:frontend_mobile/features/main/presentation/widgets/integrity_banner.dart';
import 'package:frontend_mobile/features/main/presentation/widgets/product_info_details.dart';
import 'package:frontend_mobile/features/main/presentation/widgets/trace_timeline.dart';
import 'package:frontend_mobile/features/main/presentation/widgets/direct_inputs_section.dart';
import 'package:frontend_mobile/features/review/presentation/widgets/product_reviews_section.dart';

class TraceResultPage extends StatelessWidget {
  final TraceEntity? initialTrace;

  const TraceResultPage({super.key, this.initialTrace});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<TraceBloc, TraceState>(
      builder: (context, state) {
        TraceEntity? trace = initialTrace;
        bool isVerifying = false;
        String? verificationError;

        if (state is TraceLoaded) {
          trace = state.trace;
          isVerifying = state.isVerifying;
          verificationError = state.verificationError;
        }

        if (trace == null || state is TraceLoading) {
          return Scaffold(
            appBar: AppBar(title: const Text('Kết quả truy xuất')),
            body: const Center(child: CircularProgressIndicator()),
          );
        }

        return Scaffold(
          appBar: AppBar(
            title: const Text('Kết quả truy xuất'),
            actions: [
              if (isVerifying)
                const Center(
                  child: Padding(
                    padding: EdgeInsets.only(right: 16.0),
                    child: SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                  ),
                ),
            ],
          ),
          body: SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (trace.directTrace == null)
                  IntegrityBanner(
                    trace: trace,
                    isVerifying: isVerifying,
                    verificationError: verificationError,
                  ),
                ProductInfoDetails(trace: trace),
                if (trace.productId.isNotEmpty) ...[
                  const SizedBox(height: 16),
                  ProductReviewsSummary(productId: trace.productId),
                ],
                if (trace.directTrace != null) ...[
                  const SizedBox(height: 24),
                  DirectInputsSection(trace: trace.directTrace!),
                ],
                const SizedBox(height: 24),
                const Text('Hành trình sản phẩm',
                    style:
                        TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
                const SizedBox(height: 16),
                TraceTimeline(trace: trace),
              ],
            ),
          ),
        );
      },
    );
  }
}

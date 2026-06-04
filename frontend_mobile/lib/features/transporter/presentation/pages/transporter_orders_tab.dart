import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../injection_container.dart';
import '../../data/trade_order_models.dart';
import '../../domain/usecases/get_order_details.dart';
import '../../domain/usecases/resolve_batch_name.dart';
import '../../domain/usecases/resolve_product_name.dart';
import '../../domain/usecases/resolve_user_label.dart';
import '../bloc/transporter_orders_bloc.dart';
import '../bloc/transporter_orders_event.dart';
import '../bloc/transporter_orders_state.dart';

class TransporterOrdersTab extends StatelessWidget {
  const TransporterOrdersTab({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) =>
          sl<TransporterOrdersBloc>()..add(FetchTransporterOrdersEvent()),
      child: const TransporterOrdersView(),
    );
  }
}

class TransporterOrdersView extends StatefulWidget {
  const TransporterOrdersView({super.key});

  @override
  State<TransporterOrdersView> createState() => _TransporterOrdersViewState();
}

class _TransporterOrdersViewState extends State<TransporterOrdersView> {
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 200) {
      context.read<TransporterOrdersBloc>().add(LoadMoreOrdersEvent());
    }
  }

  Future<void> _refreshOrders() async {
    final bloc = context.read<TransporterOrdersBloc>();
    bloc.add(FetchTransporterOrdersEvent());
    try {
      await bloc.stream
          .firstWhere(
            (state) =>
                state is TransporterOrdersError ||
                (state is TransporterOrdersLoaded && !state.isRefreshing),
          )
          .timeout(const Duration(seconds: 15));
    } catch (_) {
      // RefreshIndicator should stop if the network request times out.
    }
  }

  String _statusLabel(String status) {
    switch (status) {
      case 'PENDING':
        return 'Chờ xử lý';
      case 'PROCESSING':
        return 'Đang xử lý';
      case 'ACCEPTED':
        return 'Đã chấp nhận';
      case 'REJECTED':
        return 'Từ chối';
      case 'CANCELLED':
        return 'Đã hủy';
      case 'ASSIGNED_TO_CARRIER':
        return 'Chờ nhận hàng';
      case 'PICKED_UP_FROM_SELLER':
        return 'Đang giao hàng';
      case 'DELIVERED':
        return 'Đã giao';
      default:
        return status;
    }
  }

  Color _statusColor(String status) {
    switch (status) {
      case 'PENDING':
      case 'PROCESSING':
        return Colors.orange;
      case 'ACCEPTED':
        return Colors.blue;
      case 'REJECTED':
      case 'CANCELLED':
        return Colors.red;
      case 'ASSIGNED_TO_CARRIER':
        return Colors.deepPurple;
      case 'PICKED_UP_FROM_SELLER':
        return Colors.teal;
      case 'DELIVERED':
        return Colors.green;
      default:
        return Colors.blueGrey;
    }
  }

  IconData _statusIcon(String status) {
    switch (status) {
      case 'PENDING':
      case 'PROCESSING':
        return Icons.hourglass_top_rounded;
      case 'ACCEPTED':
        return Icons.check_circle_outline_rounded;
      case 'ASSIGNED_TO_CARRIER':
        return Icons.inventory_2_outlined;
      case 'PICKED_UP_FROM_SELLER':
        return Icons.local_shipping_outlined;
      case 'DELIVERED':
        return Icons.task_alt_rounded;
      case 'REJECTED':
      case 'CANCELLED':
        return Icons.cancel_outlined;
      default:
        return Icons.receipt_long_outlined;
    }
  }

  String _typeLabel(String type) {
    switch (type) {
      case 'MANUFACTURER_TO_SUPPLIER':
        return 'Nguyên liệu';
      case 'RETAILER_TO_MANUFACTURER':
        return 'Thành phẩm';
      case 'MANUFACTURER_TO_MANUFACTURER':
        return 'Pallet đầu vào';
      default:
        return type;
    }
  }

  String _nextStep(String status) {
    switch (status) {
      case 'ASSIGNED_TO_CARRIER':
        return 'Nhận hàng tại người bán';
      case 'PICKED_UP_FROM_SELLER':
        return 'Giao hàng tới người mua';
      case 'DELIVERED':
        return 'Hoàn tất vận chuyển';
      default:
        return 'Theo dõi trạng thái đơn';
    }
  }

  String _dateText(String? raw) {
    final date = DateTime.tryParse(raw ?? '')?.toLocal();
    if (date == null) return '--';
    String two(int value) => value.toString().padLeft(2, '0');
    return '${two(date.hour)}:${two(date.minute)}  ${two(date.day)}/${two(date.month)}/${date.year}';
  }

  String _quantityText(TradeOrderLineDto line) {
    if (line.quantityRequested != null) {
      return '${line.quantityRequested} ${line.unit ?? ''}'.trim();
    }
    if (line.quantityCartons != null) {
      return '${line.quantityCartons} thùng';
    }
    return '--';
  }

  Future<void> _openDetail(TradeOrderDto order) async {
    final getDetails = sl<GetOrderDetailsUseCase>();
    final resolveLabel = sl<ResolveUserLabelUseCase>();
    final resolveBatch = sl<ResolveBatchNameUseCase>();
    final resolveProduct = sl<ResolveProductNameUseCase>();

    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (_) => const Center(child: CircularProgressIndicator()),
    );

    TradeOrderDto detail = order;
    try {
      detail = await getDetails(order.id);
    } catch (_) {}

    var buyerLabel = detail.buyerId;
    var sellerLabel = detail.sellerId;
    try {
      final users = await Future.wait([
        resolveLabel(detail.buyerId),
        resolveLabel(detail.sellerId),
      ]);
      buyerLabel = users[0];
      sellerLabel = users[1];
    } catch (_) {}

    final resolvedLines = <_ResolvedLine>[];
    for (final line in detail.lines) {
      var name = 'Mặt hàng';
      try {
        if (line.targetRawBatchId != null) {
          name = await resolveBatch(line.targetRawBatchId!);
        } else if (line.productId != null) {
          name = await resolveProduct(line.productId!);
        }
      } catch (_) {}
      resolvedLines.add(_ResolvedLine(line: line, name: name));
    }

    if (!mounted) return;
    Navigator.of(context).pop();

    final bloc = context.read<TransporterOrdersBloc>();
    final messenger = ScaffoldMessenger.of(context);
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) => _buildDetailSheet(
        sheetContext,
        detail,
        buyerLabel,
        sellerLabel,
        resolvedLines,
        bloc,
        messenger,
      ),
    );
  }

  Widget _buildDetailSheet(
    BuildContext sheetContext,
    TradeOrderDto detail,
    String buyerLabel,
    String sellerLabel,
    List<_ResolvedLine> lines,
    TransporterOrdersBloc bloc,
    ScaffoldMessengerState messenger,
  ) {
    final color = _statusColor(detail.status);
    return DraggableScrollableSheet(
      expand: false,
      initialChildSize: 0.78,
      minChildSize: 0.45,
      maxChildSize: 0.94,
      builder: (_, scrollController) => Container(
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
        ),
        child: SingleChildScrollView(
          controller: scrollController,
          padding: const EdgeInsets.fromLTRB(20, 10, 20, 28),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 44,
                  height: 5,
                  margin: const EdgeInsets.only(bottom: 20),
                  decoration: BoxDecoration(
                    color: Colors.grey.shade300,
                    borderRadius: BorderRadius.circular(20),
                  ),
                ),
              ),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 48,
                    height: 48,
                    decoration: BoxDecoration(
                      color: color.withOpacity(0.12),
                      borderRadius: BorderRadius.circular(15),
                    ),
                    child: Icon(_statusIcon(detail.status), color: color),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          detail.orderCode,
                          style: const TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        const SizedBox(height: 6),
                        _StatusBadge(
                          label: _statusLabel(detail.status),
                          color: color,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              _ProgressRoute(status: detail.status),
              const SizedBox(height: 20),
              _SectionCard(
                title: 'Thông tin vận chuyển',
                child: Column(
                  children: [
                    _InfoRow(
                      icon: Icons.sell_outlined,
                      label: 'Loại hàng',
                      value: _typeLabel(detail.orderType),
                    ),
                    _InfoRow(
                      icon: Icons.schedule_outlined,
                      label: 'Tạo lúc',
                      value: _dateText(detail.createdAt),
                    ),
                    _InfoRow(
                      icon: Icons.storefront_outlined,
                      label: 'Người gửi',
                      value: sellerLabel,
                    ),
                    _InfoRow(
                      icon: Icons.location_on_outlined,
                      label: 'Người nhận',
                      value: buyerLabel,
                      last: true,
                    ),
                  ],
                ),
              ),
              if (detail.note != null && detail.note!.trim().isNotEmpty) ...[
                const SizedBox(height: 14),
                _SectionCard(
                  title: 'Ghi chú',
                  child: Text(
                    detail.note!,
                    style: TextStyle(color: Colors.grey.shade700),
                  ),
                ),
              ],
              if (lines.isNotEmpty) ...[
                const SizedBox(height: 14),
                Text(
                  'Danh sách hàng (${lines.length})',
                  style: const TextStyle(
                      fontSize: 16, fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 10),
                ...lines.map(
                  (resolved) => _LineCard(
                    index: resolved.line.lineIndex ?? 0,
                    name: resolved.name,
                    quantity: _quantityText(resolved.line),
                  ),
                ),
              ],
              if (detail.status == 'ASSIGNED_TO_CARRIER' ||
                  detail.status == 'PICKED_UP_FROM_SELLER') ...[
                const SizedBox(height: 20),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    style: FilledButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 15),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(14),
                      ),
                    ),
                    onPressed: () => _confirmAction(
                      sheetContext,
                      detail,
                      bloc,
                      messenger,
                    ),
                    icon: Icon(
                      detail.status == 'ASSIGNED_TO_CARRIER'
                          ? Icons.inventory_2_outlined
                          : Icons.task_alt_rounded,
                    ),
                    label: Text(
                      detail.status == 'ASSIGNED_TO_CARRIER'
                          ? 'Xác nhận đã nhận hàng'
                          : 'Xác nhận đã giao hàng',
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _confirmAction(
    BuildContext sheetContext,
    TradeOrderDto order,
    TransporterOrdersBloc bloc,
    ScaffoldMessengerState messenger,
  ) async {
    final isPickup = order.status == 'ASSIGNED_TO_CARRIER';
    final confirmed = await showDialog<bool>(
      context: sheetContext,
      builder: (dialogContext) => AlertDialog(
        icon: Icon(
          isPickup ? Icons.inventory_2_outlined : Icons.local_shipping_outlined,
          color: Theme.of(dialogContext).colorScheme.primary,
        ),
        title: Text(
            isPickup ? 'Xác nhận đã nhận hàng?' : 'Xác nhận đã giao hàng?'),
        content: Text(
          isPickup
              ? 'Bạn xác nhận đã nhận hàng tại người bán và bắt đầu vận chuyển?'
              : 'Bạn xác nhận hàng đã được giao tới người mua?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('Xác nhận'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    if (isPickup) {
      bloc.add(ConfirmPickedUpEvent(order));
    } else {
      bloc.add(ConfirmDeliveredEvent(order));
    }
    if (sheetContext.mounted) Navigator.pop(sheetContext);
    messenger.showSnackBar(
      const SnackBar(content: Text('Đã gửi yêu cầu cập nhật vận chuyển')),
    );
  }

  Widget _buildSummary(List<TradeOrderDto> orders) {
    final active = orders
        .where((o) =>
            o.status == 'ASSIGNED_TO_CARRIER' ||
            o.status == 'PICKED_UP_FROM_SELLER')
        .length;
    final delivered = orders.where((o) => o.status == 'DELIVERED').length;
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF147D52), Color(0xFF27A86B)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(22),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.local_shipping_rounded, color: Colors.white),
              SizedBox(width: 8),
              Text(
                'Chuyến giao của bạn',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              _SummaryPill(label: 'Đang thực hiện', value: '$active'),
              const SizedBox(width: 10),
              _SummaryPill(label: 'Đã giao', value: '$delivered'),
            ],
          ),
          const SizedBox(height: 14),
          const Text(
            'Kéo xuống để cập nhật danh sách mới nhất',
            style: TextStyle(color: Colors.white70, fontSize: 12),
          ),
        ],
      ),
    );
  }

  Widget _buildOrderCard(TradeOrderDto order) {
    final color = _statusColor(order.status);
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(18),
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: () => _openDetail(order),
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey.shade200),
            borderRadius: BorderRadius.circular(18),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      order.orderCode,
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 16,
                      ),
                    ),
                  ),
                  _StatusBadge(label: _statusLabel(order.status), color: color),
                ],
              ),
              const SizedBox(height: 14),
              Row(
                children: [
                  Expanded(
                    child: _MiniInfo(
                      icon: Icons.inventory_2_outlined,
                      value: _typeLabel(order.orderType),
                    ),
                  ),
                  const SizedBox(width: 10),
                  _MiniInfo(
                    icon: Icons.layers_outlined,
                    value: '${order.lines.length} mặt hàng',
                  ),
                ],
              ),
              const SizedBox(height: 14),
              Divider(height: 1, color: Colors.grey.shade200),
              const SizedBox(height: 12),
              Row(
                children: [
                  Icon(_statusIcon(order.status), color: color, size: 19),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      _nextStep(order.status),
                      style: TextStyle(
                        color: Colors.grey.shade700,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                  Icon(Icons.chevron_right_rounded,
                      color: Colors.grey.shade400),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<TransporterOrdersBloc, TransporterOrdersState>(
      listener: (context, state) {
        if (state is TransporterOrdersError) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(state.message),
              backgroundColor: Colors.redAccent,
            ),
          );
        }
      },
      builder: (context, state) {
        if (state is TransporterOrdersLoading ||
            state is TransporterOrdersInitial) {
          return const Center(child: CircularProgressIndicator());
        }
        if (state is TransporterOrdersError) {
          return _ErrorState(onRetry: _refreshOrders, message: state.message);
        }
        if (state is! TransporterOrdersLoaded) return const SizedBox();

        final orders = state.displayOrders;
        return Stack(
          children: [
            RefreshIndicator(
              onRefresh: _refreshOrders,
              child: ListView.builder(
                controller: _scrollController,
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 88),
                itemCount: 1 +
                    (orders.isEmpty ? 1 : orders.length) +
                    (state.isLoadingMore ? 1 : 0),
                itemBuilder: (context, index) {
                  if (index == 0) {
                    return _buildSummary(state.allOrders);
                  }
                  if (orders.isEmpty) {
                    return const _EmptyState();
                  }
                  final orderIndex = index - 1;
                  if (orderIndex == orders.length) {
                    return const Padding(
                      padding: EdgeInsets.all(18),
                      child: Center(child: CircularProgressIndicator()),
                    );
                  }
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: _buildOrderCard(orders[orderIndex]),
                  );
                },
              ),
            ),
            if (state.isRefreshing)
              const Positioned(
                top: 0,
                left: 0,
                right: 0,
                child: LinearProgressIndicator(minHeight: 2),
              ),
          ],
        );
      },
    );
  }
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.label, required this.color});

  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: color.withOpacity(0.12),
        borderRadius: BorderRadius.circular(30),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: color,
          fontSize: 12,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _SummaryPill extends StatelessWidget {
  const _SummaryPill({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 10),
        decoration: BoxDecoration(
          color: Colors.white.withOpacity(0.15),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              value,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 22,
                fontWeight: FontWeight.bold,
              ),
            ),
            Text(label, style: const TextStyle(color: Colors.white70)),
          ],
        ),
      ),
    );
  }
}

class _MiniInfo extends StatelessWidget {
  const _MiniInfo({required this.icon, required this.value});

  final IconData icon;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, color: Colors.grey.shade500, size: 17),
        const SizedBox(width: 6),
        Text(
          value,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: TextStyle(color: Colors.grey.shade700, fontSize: 13),
        ),
      ],
    );
  }
}

class _SectionCard extends StatelessWidget {
  const _SectionCard({required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAF9),
        borderRadius: BorderRadius.circular(17),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontWeight: FontWeight.w700)),
          const SizedBox(height: 10),
          child,
        ],
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({
    required this.icon,
    required this.label,
    required this.value,
    this.last = false,
  });

  final IconData icon;
  final String label;
  final String value;
  final bool last;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: last ? 0 : 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 18, color: Colors.grey.shade600),
          const SizedBox(width: 10),
          SizedBox(
            width: 92,
            child: Text(label, style: TextStyle(color: Colors.grey.shade600)),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }
}

class _ProgressRoute extends StatelessWidget {
  const _ProgressRoute({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    final pickupDone =
        status == 'PICKED_UP_FROM_SELLER' || status == 'DELIVERED';
    final deliveryDone = status == 'DELIVERED';
    return Row(
      children: [
        const _RouteNode(
          icon: Icons.storefront_outlined,
          label: 'Người bán',
          active: true,
        ),
        _RouteLine(active: pickupDone),
        _RouteNode(
          icon: Icons.local_shipping_outlined,
          label: 'Đang giao',
          active: pickupDone,
        ),
        _RouteLine(active: deliveryDone),
        _RouteNode(
          icon: Icons.home_outlined,
          label: 'Người mua',
          active: deliveryDone,
        ),
      ],
    );
  }
}

class _RouteNode extends StatelessWidget {
  const _RouteNode({
    required this.icon,
    required this.label,
    required this.active,
  });

  final IconData icon;
  final String label;
  final bool active;

  @override
  Widget build(BuildContext context) {
    final color = active ? Theme.of(context).colorScheme.primary : Colors.grey;
    return Column(
      children: [
        CircleAvatar(
          radius: 21,
          backgroundColor: color.withOpacity(0.12),
          child: Icon(icon, color: color, size: 21),
        ),
        const SizedBox(height: 6),
        Text(
          label,
          style: TextStyle(
              color: color, fontSize: 11, fontWeight: FontWeight.w600),
        ),
      ],
    );
  }
}

class _RouteLine extends StatelessWidget {
  const _RouteLine({required this.active});

  final bool active;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        height: 3,
        margin: const EdgeInsets.fromLTRB(7, 0, 7, 20),
        decoration: BoxDecoration(
          color: active
              ? Theme.of(context).colorScheme.primary
              : Colors.grey.shade200,
          borderRadius: BorderRadius.circular(4),
        ),
      ),
    );
  }
}

class _LineCard extends StatelessWidget {
  const _LineCard({
    required this.index,
    required this.name,
    required this.quantity,
  });

  final int index;
  final String name;
  final String quantity;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(13),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Row(
        children: [
          CircleAvatar(
            radius: 16,
            backgroundColor: Theme.of(context).colorScheme.primaryContainer,
            child: Text(
              '${index + 1}',
              style: TextStyle(
                color: Theme.of(context).colorScheme.onPrimaryContainer,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child:
                Text(name, style: const TextStyle(fontWeight: FontWeight.w600)),
          ),
          Text(quantity, style: TextStyle(color: Colors.grey.shade700)),
        ],
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 54, horizontal: 24),
      child: Column(
        children: [
          Icon(Icons.local_shipping_outlined,
              size: 54, color: Colors.grey.shade300),
          const SizedBox(height: 14),
          const Text(
            'Chưa có đơn vận chuyển',
            style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
          ),
          const SizedBox(height: 6),
          Text(
            'Các đơn được gán cho bạn sẽ hiển thị tại đây.',
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.grey.shade600),
          ),
        ],
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.onRetry, required this.message});

  final Future<void> Function() onRetry;
  final String message;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.cloud_off_outlined,
                size: 54, color: Colors.red.shade300),
            const SizedBox(height: 12),
            const Text(
              'Không tải được đơn vận chuyển',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 6),
            Text(
              message,
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey.shade600),
            ),
            const SizedBox(height: 18),
            FilledButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Thử lại'),
            ),
          ],
        ),
      ),
    );
  }
}

class _ResolvedLine {
  const _ResolvedLine({required this.line, required this.name});

  final TradeOrderLineDto line;
  final String name;
}

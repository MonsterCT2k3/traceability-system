import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../../injection_container.dart';
import '../../bloc/history/history_bloc.dart';
import '../../bloc/history/history_event.dart';
import '../../bloc/history/history_state.dart';
import '../../bloc/trace/trace_bloc.dart';
import '../../bloc/trace/trace_event.dart';
import '../trace_result_page.dart';

class HistoryTab extends StatefulWidget {
  const HistoryTab({super.key});

  @override
  State<HistoryTab> createState() => _HistoryTabState();
}

class _HistoryTabState extends State<HistoryTab> {
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    context.read<HistoryBloc>().add(const LoadHistoryEvent(page: 0));
    _scrollController.addListener(_onScroll);
  }

  void _onScroll() {
    if (_scrollController.position.pixels >= _scrollController.position.maxScrollExtent - 200) {
      final state = context.read<HistoryBloc>().state;
      if (state is HistoryLoaded && !state.hasReachedMax) {
        final nextPage = (state.historyList.length / 20).ceil();
        context.read<HistoryBloc>().add(LoadHistoryEvent(page: nextPage));
      }
    }
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _navigateToTrace(String serial) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => BlocProvider<TraceBloc>(
          create: (context) => sl<TraceBloc>()
            ..add(GetTraceDetails(serial, isHistory: true)),
          child: const TraceResultPage(),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Lịch sử truy xuất'),
        centerTitle: true,
      ),
      body: BlocBuilder<HistoryBloc, HistoryState>(
        builder: (context, state) {
          if (state is HistoryLoading) {
            return const Center(child: CircularProgressIndicator());
          } else if (state is HistoryError) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text('Lỗi: ${state.message}', style: const TextStyle(color: Colors.red)),
                  const SizedBox(height: 16),
                  ElevatedButton(
                    onPressed: () => context.read<HistoryBloc>().add(const LoadHistoryEvent(page: 0)),
                    child: const Text('Thử lại'),
                  ),
                ],
              ),
            );
          } else if (state is HistoryLoaded) {
            if (state.historyList.isEmpty) {
              return const Center(
                child: Text(
                  'Bạn chưa quét sản phẩm nào.',
                  style: TextStyle(fontSize: 16, color: Colors.grey),
                ),
              );
            }

            return RefreshIndicator(
              onRefresh: () async {
                context.read<HistoryBloc>().add(const LoadHistoryEvent(page: 0));
              },
              child: ListView.separated(
                controller: _scrollController,
                itemCount: state.historyList.length + (state.hasReachedMax ? 0 : 1),
                separatorBuilder: (context, index) => const Divider(height: 1),
                itemBuilder: (context, index) {
                  if (index >= state.historyList.length) {
                    return const Padding(
                      padding: EdgeInsets.symmetric(vertical: 16.0),
                      child: Center(child: CircularProgressIndicator()),
                    );
                  }

                  final item = state.historyList[index];
                  String pad(int n) => n.toString().padLeft(2, '0');
                  final d = item.scannedAt;
                  final formattedDate = '${pad(d.day)}/${pad(d.month)}/${d.year} ${pad(d.hour)}:${pad(d.minute)}:${pad(d.second)}';

                  return ListTile(
                    contentPadding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
                    leading: Container(
                      width: 50,
                      height: 50,
                      decoration: BoxDecoration(
                        color: Colors.grey[200],
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: item.productImage != null && item.productImage!.isNotEmpty
                          ? ClipRRect(
                              borderRadius: BorderRadius.circular(8),
                              child: Image.network(
                                item.productImage!,
                                fit: BoxFit.cover,
                                errorBuilder: (c, o, s) => const Icon(Icons.inventory_2, color: Colors.grey),
                              ),
                            )
                          : const Icon(Icons.inventory_2, color: Colors.grey),
                    ),
                    title: Text(
                      item.productName ?? 'Sản phẩm không rõ',
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    subtitle: Padding(
                      padding: const EdgeInsets.only(top: 4.0),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Seri: ${item.unitSerial}', style: const TextStyle(fontSize: 13)),
                          const SizedBox(height: 2),
                          Row(
                            children: [
                              const Icon(Icons.access_time, size: 12, color: Colors.grey),
                              const SizedBox(width: 4),
                              Text(formattedDate, style: const TextStyle(fontSize: 12, color: Colors.grey)),
                            ],
                          ),
                        ],
                      ),
                    ),
                    trailing: const Icon(Icons.chevron_right, color: Colors.grey),
                    onTap: () => _navigateToTrace(item.unitSerial),
                  );
                },
              ),
            );
          }
          return const SizedBox.shrink();
        },
      ),
    );
  }
}

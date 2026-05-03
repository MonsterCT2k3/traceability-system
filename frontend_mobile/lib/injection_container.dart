import 'package:dio/dio.dart';
import 'package:get_it/get_it.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'core/network/api_client.dart';
import 'features/auth/data/repositories/auth_repository_impl.dart';
import 'features/auth/domain/repositories/auth_repository.dart';
import 'features/auth/domain/usecases/login_usecase.dart';
import 'features/auth/presentation/bloc/auth_bloc.dart';
import 'features/main/data/datasources/trace_remote_datasource.dart';
import 'features/main/data/repositories/trace_repository_impl.dart';
import 'features/main/domain/repositories/trace_repository.dart';
import 'features/main/domain/usecases/get_trace_by_serial.dart';
import 'features/main/domain/usecases/verify_trace_on_chain.dart';
import 'features/main/presentation/bloc/trace/trace_bloc.dart';

final sl = GetIt.instance;

Future<void> init() async {
  // Features - Auth
  // Bloc
  sl.registerFactory(() => AuthBloc(loginUseCase: sl()));

  // Use cases
  sl.registerLazySingleton(() => LoginUseCase(sl()));

  // Repository
  sl.registerLazySingleton<AuthRepository>(
    () => AuthRepositoryImpl(apiClient: sl()),
  );

  // Features - Main (Traceability)
  // Bloc
  sl.registerFactory(
    () => TraceBloc(
      getTraceBySerial: sl(),
      verifyTraceOnChain: sl(),
    ),
  );

  // Use cases
  sl.registerLazySingleton(() => GetTraceBySerialUseCase(sl()));
  sl.registerLazySingleton(() => VerifyTraceOnChainUseCase(sl()));

  // Repository
  sl.registerLazySingleton<TraceRepository>(
    () => TraceRepositoryImpl(remoteDataSource: sl()),
  );

  // Data sources
  sl.registerLazySingleton<TraceRemoteDataSource>(
    () => TraceRemoteDataSourceImpl(apiClient: sl()),
  );

  // Core
  sl.registerLazySingleton(() => ApiClient(dio: sl(), sharedPreferences: sl()));

  // External (Third Party)
  final sharedPreferences = await SharedPreferences.getInstance();
  sl.registerLazySingleton(() => sharedPreferences);
  sl.registerLazySingleton(() => Dio());
}

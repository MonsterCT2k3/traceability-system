import 'package:flutter_bloc/flutter_bloc.dart';
import 'auth_event.dart';
import 'auth_state.dart';
import '../../domain/usecases/login_usecase.dart';
import '../../domain/repositories/auth_repository.dart';
import '../../../../injection_container.dart';

class AuthBloc extends Bloc<AuthEvent, AuthState> {
  final LoginUseCase loginUseCase;

  AuthBloc({required this.loginUseCase}) : super(AuthInitial()) {
    on<LoginEvent>(_onLogin);
    on<LogoutEvent>(_onLogout);
    on<UpdateProfileEvent>(_onUpdateProfile);
    on<UpdateAvatarEvent>(_onUpdateAvatar);
  }

  Future<void> _onUpdateProfile(UpdateProfileEvent event, Emitter<AuthState> emit) async {
    if (state is AuthAuthenticated) {
      final currentUser = (state as AuthAuthenticated).user;
      emit(AuthLoading());
      final authRepository = sl<AuthRepository>();
      final result = await authRepository.updateProfile(event.fullName, event.email, event.phone, event.description, event.location);
      
      result.fold(
        (failure) {
          emit(AuthError(message: failure.message));
          emit(AuthAuthenticated(user: currentUser)); // Revert back to old state
        },
        (updatedUser) => emit(AuthAuthenticated(user: updatedUser)),
      );
    }
  }

  Future<void> _onUpdateAvatar(UpdateAvatarEvent event, Emitter<AuthState> emit) async {
    if (state is AuthAuthenticated) {
      final currentUser = (state as AuthAuthenticated).user;
      emit(AuthLoading());
      final authRepository = sl<AuthRepository>();
      final result = await authRepository.updateAvatar(event.imageFile);
      
      result.fold(
        (failure) {
          emit(AuthError(message: failure.message));
          emit(AuthAuthenticated(user: currentUser)); // Revert back to old state
        },
        (updatedUser) => emit(AuthAuthenticated(user: updatedUser)),
      );
    }
  }

  Future<void> _onLogin(LoginEvent event, Emitter<AuthState> emit) async {
    emit(AuthLoading());
    final result = await loginUseCase.call(LoginParams(username: event.username, password: event.password));
    
    result.fold(
      (failure) => emit(AuthError(message: failure.message)),
      (user) => emit(AuthAuthenticated(user: user)),
    );
  }

  Future<void> _onLogout(LogoutEvent event, Emitter<AuthState> emit) async {
    final authRepository = sl<AuthRepository>();
    await authRepository.logout();
    emit(AuthUnauthenticated());
  }
}

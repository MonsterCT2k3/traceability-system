export const AUTH_SESSION_CHANGED_EVENT = 'auth-session-changed';

export const storeAuthSession = ({ accessToken, refreshToken, userRole }) => {
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
  localStorage.setItem('userRole', userRole);
  window.dispatchEvent(new Event(AUTH_SESSION_CHANGED_EVENT));
};

export const clearAuthSession = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('userRole');
  sessionStorage.removeItem('pendingRawBatchCreationId');
  window.dispatchEvent(new Event(AUTH_SESSION_CHANGED_EVENT));
};

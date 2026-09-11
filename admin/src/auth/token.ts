const KEY = 'modu-admin-token'
const REFRESH_KEY = 'modu-admin-refresh-token'
export const getToken = () => localStorage.getItem(KEY)
export const setToken = (t: string) => localStorage.setItem(KEY, t)
export const getRefreshToken = () => localStorage.getItem(REFRESH_KEY)
export const setRefreshToken = (t: string) => localStorage.setItem(REFRESH_KEY, t)
export const clearToken = () => {
  localStorage.removeItem(KEY)
  localStorage.removeItem(REFRESH_KEY)
}

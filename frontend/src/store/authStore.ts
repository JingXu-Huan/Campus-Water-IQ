import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AuthState {
  token: string | null
  uid: string | null
  nickname: string | null
  setAuth: (token: string, uid: string, nickname?: string) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      uid: null,
      nickname: null,
      setAuth: (token, uid, nickname) => set({ token, uid, nickname }),
      clearAuth: () => set({ token: null, uid: null, nickname: null }),
    }),
    {
      name: 'auth-storage',
    }
  )
)

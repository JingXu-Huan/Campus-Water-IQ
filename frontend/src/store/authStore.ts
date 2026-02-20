import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AuthState {
  token: string | null
  uid: string | null
  nickname: string | null
  avatar: string | null
  setAuth: (token: string, uid: string, nickname?: string, avatar?: string) => void
  updateProfile: (nickname?: string, avatar?: string) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set): AuthState => ({
      token: null,
      uid: null,
      nickname: null,
      avatar: null,
      setAuth: (token, uid, nickname, avatar) => set({ token, uid, nickname, avatar }),
      updateProfile: (nickname, avatar) => set((state) => ({ 
        nickname: nickname ?? state.nickname,
        avatar: avatar ?? state.avatar 
      })),
      clearAuth: () => set({ token: null, uid: null, nickname: null, avatar: null }),
    }),
    {
      name: 'auth-storage',
    }
  )
)

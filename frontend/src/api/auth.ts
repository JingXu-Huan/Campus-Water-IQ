import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 用于文件上传的 axios 实例
const fileApi = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// 提取 token 的辅助函数
const getToken = () => {
  try {
    const authData = localStorage.getItem('auth-storage')
    if (authData) {
      const parsed = JSON.parse(authData)
      return parsed.state?.token || parsed.token || null
    }
  } catch (e) {
    console.error('Failed to parse auth token:', e)
  }
  return null
}

fileApi.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

fileApi.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message = error.response?.data?.message || '请求失败，请稍后重试'
    return Promise.reject(new Error(message))
  }
)

api.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message = error.response?.data?.message || '请求失败，请稍后重试'
    return Promise.reject(new Error(message))
  }
)

export interface SignInRequest {
  type: 'PASSWORD' | 'PHONE' | 'EMAIL' | 'WECHAT' | 'QQ' | 'GITHUB'
  identifier: string
  credential: string
}

export interface SignUpRequest {
  type: 'PHONE_AND_CODE' | 'MAIL_AND_CODE' | 'GITHUB_OAUTH'
  identifier: string
  credential: string
  pwd: string
  nickname: string
  role: number
}

export interface AuthResult {
  success: boolean
  uid?: string
  token?: string
  nickName?: string
  avatarURL?: string
}

export interface SignUpResult {
  isSuccess: boolean
  uid?: string
  nickName?: string
  msg?: string
}

export interface UserProfile {
  uid: string
  nickName: string
  avatar?: string
  email?: string
  phone?: string
}

export interface UpdateNicknameRequest {
  nickname: string
}

export interface UpdatePasswordRequest {
  oldPassword: string
  newPassword: string
}

export interface UpdateAvatarRequest {
  avatar: string  // base64 or URL
}

export const authApi = {
  signIn: (data: SignInRequest) => api.post<AuthResult>('/auth/signin', data),
  
  signUp: (data: SignUpRequest) => api.put<SignUpResult>('/signup', data),
  
  sendEmailCode: (toEmail: string) => 
    api.post('/auth/send-code', { toEmail }),
  
  sendPhoneCode: (phoneNum: string) => 
    api.post('/auth/send-phone-code', { phoneNum }),
  
  sendSignUpEmailCode: (toEmail: string) => 
    api.post('/signup/send-code', { toEmail }),
  
  getGitHubAuthUrl: () => api.get<string>('/auth/github/authorize'),
  
  getWeChatAuthUrl: () => api.get<string>('/auth/wechat/authorize'),
  
  // 获取用户信息
  getUserProfile: () => api.get<UserProfile>('/user/profile'),
  
  // 更新昵称
  updateNickname: (newName: string, uid: string) => 
    api.post('/user/changeNickName', {}, { params: { newName, uid } }),
  
  // 更新密码
  updatePassword: (data: UpdatePasswordRequest) => 
    api.put('/user/password', data),
  
  // 更新头像
  updateAvatar: (data: UpdateAvatarRequest) => 
    api.put('/user/avatar', data),
  
  // 上传头像 (MultipartFile)
  uploadAvatar: (file: File, uid: string) => {
    const formData = new FormData()
    formData.append('image', file)
    formData.append('uid', uid)
    return fileApi.post('/user/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },
  
  // 获取头像地址
  getAvatar: (uid: string) => api.get<string>('/user/getAvatat', { params: { uid } }),
}

export default api

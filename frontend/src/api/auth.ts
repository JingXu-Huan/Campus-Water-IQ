import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth-token')
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
  type: 'PHONE' | 'EMAIL'
  identifier: string
  credential: string
  pwd: string
  nickname: string
}

export interface AuthResult {
  success: boolean
  uid?: string
  token?: string
}

export interface SignUpResult {
  isSuccess: boolean
  uid?: string
  nickName?: string
  msg?: string
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
}

export default api

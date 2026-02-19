import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authApi, SignUpRequest } from '@/api/auth'
import { Eye, EyeOff, Droplets, Mail, Phone, Lock, User, MessageSquare } from 'lucide-react'

type RegisterMethod = 'email' | 'phone'

export default function Register() {
  const navigate = useNavigate()
  
  const [registerMethod, setRegisterMethod] = useState<RegisterMethod>('email')
  const [identifier, setIdentifier] = useState('')
  const [credential, setCredential] = useState('')
  const [confirmPwd, setConfirmPwd] = useState('')
  const [nickname, setNickname] = useState('')
  const [code, setCode] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [sendingCode, setSendingCode] = useState(false)
  const [countdown, setCountdown] = useState(0)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const sendCode = async () => {
    if (!identifier) {
      setError('请先输入邮箱地址')
      return
    }
    
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(identifier)) {
      setError('请输入有效的邮箱地址')
      return
    }

    setSendingCode(true)
    setError('')
    try {
      await authApi.sendSignUpEmailCode(identifier)
      setSuccess('验证码已发送到您的邮箱')
      setCountdown(60)
      const timer = setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1) {
            clearInterval(timer)
            return 0
          }
          return prev - 1
        })
      }, 1000)
    } catch (err) {
      setError(err instanceof Error ? err.message : '发送失败，请稍后重试')
    } finally {
      setSendingCode(false)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    
    if (!nickname.trim()) {
      setError('请输入昵称')
      return
    }
    if (!identifier) {
      setError('请输入邮箱')
      return
    }
    if (!code) {
      setError('请输入验证码')
      return
    }
    if (!credential) {
      setError('请输入密码')
      return
    }
    if (credential.length < 6) {
      setError('密码长度至少6位')
      return
    }
    if (credential !== confirmPwd) {
      setError('两次输入的密码不一致')
      return
    }

    setLoading(true)
    try {
      const requestData: SignUpRequest = {
        type: registerMethod === 'email' ? 'EMAIL' : 'PHONE',
        identifier,
        credential: code,
        pwd: credential,
        nickname
      }

      const res = await authApi.signUp(requestData)
      
      if (res.isSuccess) {
        setSuccess('注册成功，正在跳转...')
        setTimeout(() => navigate('/login'), 1500)
      } else {
        setError(res.msg || '注册失败')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '注册失败，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-2xl shadow-xl p-8">
          <div className="text-center mb-8">
            <div className="inline-flex items-center justify-center w-16 h-16 bg-primary-100 rounded-full mb-4">
              <Droplets className="w-8 h-8 text-primary-600" />
            </div>
            <h1 className="text-2xl font-bold text-gray-900">用户注册</h1>
            <p className="text-gray-500 mt-1">加入校园水务数字孪生平台</p>
          </div>

          <div className="flex rounded-lg bg-gray-100 p-1 mb-6">
            {(['email', 'phone'] as RegisterMethod[]).map((method) => (
              <button
                key={method}
                type="button"
                onClick={() => setRegisterMethod(method)}
                className={`flex-1 py-2 px-4 rounded-md text-sm font-medium transition-all ${
                  registerMethod === method
                    ? 'bg-white text-primary-600 shadow-sm'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
              >
                {method === 'email' ? '邮箱注册' : '手机注册'}
              </button>
            ))}
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">昵称</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                  <User className="w-5 h-5" />
                </div>
                <input
                  type="text"
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                  placeholder="请输入昵称"
                  className="w-full pl-10 pr-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none transition-all"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {registerMethod === 'email' ? '邮箱' : '手机号'}
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                  {registerMethod === 'email' ? <Mail className="w-5 h-5" /> : <Phone className="w-5 h-5" />}
                </div>
                <input
                  type="text"
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  placeholder={registerMethod === 'email' ? '请输入邮箱' : '请输入手机号'}
                  className="w-full pl-10 pr-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none transition-all"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">验证码</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                  <MessageSquare className="w-5 h-5" />
                </div>
                <input
                  type="text"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="请输入验证码"
                  className="w-full pl-10 pr-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none transition-all"
                />
                <button
                  type="button"
                  onClick={sendCode}
                  disabled={countdown > 0 || sendingCode}
                  className="absolute inset-y-0 right-0 pr-3 text-sm text-primary-600 hover:text-primary-700 disabled:text-gray-400 disabled:cursor-not-allowed"
                >
                  {countdown > 0 ? `${countdown}s` : sendingCode ? '发送中...' : '获取验证码'}
                </button>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">密码</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                  <Lock className="w-5 h-5" />
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={credential}
                  onChange={(e) => setCredential(e.target.value)}
                  placeholder="请输入密码（至少6位）"
                  className="w-full pl-10 pr-12 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none transition-all"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
                >
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">确认密码</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                  <Lock className="w-5 h-5" />
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={confirmPwd}
                  onChange={(e) => setConfirmPwd(e.target.value)}
                  placeholder="请再次输入密码"
                  className="w-full pl-10 pr-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none transition-all"
                />
              </div>
            </div>

            {error && (
              <div className="text-red-500 text-sm text-center bg-red-50 py-2 rounded-lg">
                {error}
              </div>
            )}
            
            {success && (
              <div className="text-green-500 text-sm text-center bg-green-50 py-2 rounded-lg">
                {success}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 px-4 bg-primary-600 hover:bg-primary-700 text-white font-medium rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? '注册中...' : '注册'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <span className="text-gray-500">已有账号？</span>
            <Link to="/login" className="text-primary-600 hover:text-primary-700 font-medium ml-1">
              立即登录
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

import { useState, useEffect, useRef } from 'react'
import { useNavigate, Link, useSearchParams } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { authApi, SignInRequest } from '@/api/auth'
import { Eye, EyeOff, Droplets, Mail, Phone, Lock, MessageSquare } from 'lucide-react'

type LoginMethod = 'password' | 'email' | 'phone'

export default function Login() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((state) => state.setAuth)
  
  const [loginMethod, setLoginMethod] = useState<LoginMethod>('password')
  const [identifier, setIdentifier] = useState('')
  const [credential, setCredential] = useState('')
  const [code, setCode] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [codeSending, setCodeSending] = useState(false)
  const [countdown, setCountdown] = useState(0)
  const [error, setError] = useState('')
  const [searchParams] = useSearchParams()
  const oauthHandled = useRef(false)

  // Handle OAuth callbacks - 只在组件挂载时执行一次
  useEffect(() => {
    // 防止 Strict Mode 导致的重复执行
    if (oauthHandled.current) {
      return
    }
    
    const githubCode = searchParams.get('github_code')
    const wechatCode = searchParams.get('wechat_code')
    const oauthError = searchParams.get('error')

    if (oauthError) {
      setError('OAuth授权失败，请重试')
      oauthHandled.current = true
      window.history.replaceState({}, '', '/login')
      return
    }

    if (githubCode) {
      oauthHandled.current = true
      // 先清理 URL，再发起请求
      window.history.replaceState({}, '', '/login')
      // 使用 setTimeout 确保状态更新后再执行
      setTimeout(() => handleOAuthLogin('GITHUB', githubCode), 0)
    } else if (wechatCode) {
      oauthHandled.current = true
      window.history.replaceState({}, '', '/login')
      setTimeout(() => handleOAuthLogin('WECHAT', wechatCode), 0)
    }
  }, [])

  const handleOAuthLogin = async (type: 'GITHUB' | 'WECHAT', code: string) => {
    setLoading(true)
    setError('')
    
    try {
      const requestData: SignInRequest = {
        type,
        identifier: '', // OAuth doesn't need identifier
        credential: code
      }

      const res = await authApi.signIn(requestData)
      
      if (res.success && res.token) {
        setAuth(res.token, res.uid || '', '')
        navigate('/dashboard')
      } else {
        setError('OAuth登录失败，请重试')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'OAuth登录失败，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    
    if (!identifier) {
      setError('请输入账号')
      return
    }
    if (!credential && loginMethod !== 'email') {
      setError('请输入密码或验证码')
      return
    }

    setLoading(true)
    try {
      const typeMap: Record<LoginMethod, SignInRequest['type']> = {
        password: 'PASSWORD',
        email: 'EMAIL',
        phone: 'PHONE'
      }

      const requestData: SignInRequest = {
        type: typeMap[loginMethod],
        identifier,
        credential: loginMethod === 'email' ? code : credential
      }

      const res = await authApi.signIn(requestData)
      
      if (res.success && res.token) {
        setAuth(res.token, res.uid || '', '')
        navigate('/dashboard')
      } else {
        setError('登录失败，请检查账号密码')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '登录失败，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  const handleSendCode = async () => {
    if (!identifier) {
      setError('请输入邮箱地址')
      return
    }
    
    if (!identifier.includes('@')) {
      setError('请输入有效的邮箱地址')
      return
    }

    setCodeSending(true)
    setError('')
    
    try {
      await authApi.sendEmailCode(identifier)
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
      setError(err instanceof Error ? err.message : '发送验证码失败，请稍后重试')
    } finally {
      setCodeSending(false)
    }
  }

  const handleGitHubLogin = async () => {
    try {
      const res = await authApi.getGitHubAuthUrl()
      if (res) {
        window.location.href = res
      }
    } catch (err) {
      setError('GitHub登录失败，请稍后重试')
    }
  }

  const handleWeChatLogin = async () => {
    try {
      const res = await authApi.getWeChatAuthUrl()
      if (res) {
        window.location.href = res
      }
    } catch (err) {
      setError('微信登录失败，请稍后重试')
    }
  }

  const getLoginMethodIcon = () => {
    switch (loginMethod) {
      case 'email': return <Mail className="w-5 h-5" />
      case 'phone': return <Phone className="w-5 h-5" />
      default: return <Mail className="w-5 h-5" />
    }
  }

  const getPlaceholder = () => {
    switch (loginMethod) {
      case 'email': return '请输入邮箱'
      case 'phone': return '请输入手机号'
      default: return '请输入Uid'
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
            <h1 className="text-2xl font-bold text-gray-900">校园水务数字孪生平台</h1>
            <p className="text-gray-500 mt-1">智慧用水监测与决策平台</p>
          </div>

          <div className="flex rounded-lg bg-gray-100 p-1 mb-6">
            {(['password', 'email', 'phone'] as LoginMethod[]).map((method) => (
              <button
                key={method}
                type="button"
                onClick={() => setLoginMethod(method)}
                className={`flex-1 py-2 px-4 rounded-md text-sm font-medium transition-all ${
                  loginMethod === method
                    ? 'bg-white text-primary-600 shadow-sm'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
              >
                {method === 'password' ? '密码登录' : method === 'email' ? '邮箱登录' : '手机登录'}
              </button>
            ))}
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {loginMethod === 'phone' ? '手机号' : loginMethod === 'email' ? '邮箱' : '账号'}
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                  {getLoginMethodIcon()}
                </div>
                <input
                  type="text"
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  placeholder={getPlaceholder()}
                  className="w-full pl-10 pr-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none transition-all"
                />
              </div>
            </div>

            {loginMethod === 'email' ? (
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
                    placeholder="请输入邮箱验证码"
                    className="w-full pl-10 pr-32 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none transition-all"
                  />
                  <button
                    type="button"
                    onClick={handleSendCode}
                    disabled={codeSending || countdown > 0 || !identifier}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center"
                  >
                    <span className={`text-sm font-medium px-3 py-1 rounded-md transition-colors ${
                      codeSending || countdown > 0 || !identifier
                        ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                        : 'bg-primary-100 text-primary-600 hover:bg-primary-200'
                    }`}>
                      {codeSending ? '发送中...' : countdown > 0 ? `${countdown}s` : '获取验证码'}
                    </span>
                  </button>
                </div>
              </div>
            ) : (
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
                    placeholder="请输入密码"
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
            )}

            {error && (
              <div className="text-red-500 text-sm text-center bg-red-50 py-2 rounded-lg">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 px-4 bg-primary-600 hover:bg-primary-700 text-white font-medium rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? '登录中...' : '登录'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <span className="text-gray-500">还没有账号？</span>
            <Link to="/register" className="text-primary-600 hover:text-primary-700 font-medium ml-1">
              立即注册
            </Link>
          </div>

          <div className="mt-6 pt-6 border-t border-gray-100">
            <p className="text-center text-sm text-gray-500 mb-4">其他登录方式</p>
            <div className="flex justify-center gap-4">
              <button
                type="button"
                onClick={handleGitHubLogin}
                className="flex items-center gap-2 px-4 py-2 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
              >
                <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
                </svg>
                <span className="text-sm">GitHub</span>
              </button>
              <button
                type="button"
                onClick={handleWeChatLogin}
                className="flex items-center gap-2 px-4 py-2 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
              >
                <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.045c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.327-1.233a.582.582 0 0 1-.023-.156.49.49 0 0 1 .201-.398C23.024 18.48 24 16.82 24 14.98c0-3.21-2.931-5.837-6.656-6.088V8.89c-.135-.007-.264-.03-.406-.03zm-2.53 3.274c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.969-.982z"/>
                </svg>
                <span className="text-sm">微信</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

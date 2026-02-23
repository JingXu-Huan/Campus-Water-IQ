import { useState, useEffect, useRef } from 'react'
import { useNavigate, Link, useSearchParams } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { authApi, SignInRequest, AuthResult } from '@/api/auth'
import { Eye, EyeOff, Droplets, Mail, Phone, Lock, MessageSquare, Loader2 } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'

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

  useEffect(() => {
    if (oauthHandled.current) return
    
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
      window.history.replaceState({}, '', '/login')
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
      const res = await authApi.signIn({ type, identifier: '', credential: code }) as unknown as AuthResult
      if (res.success && res.token) {
        setAuth(res.token, res.uid || '', res.nickName || '', res.avatarURL || '')
        navigate('/digital-twin')
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

      const res = await authApi.signIn({
        type: typeMap[loginMethod],
        identifier,
        credential: loginMethod === 'email' ? code : credential
      }) as unknown as AuthResult
      
      if (res.success && res.token) {
        setAuth(res.token, res.uid || '', res.nickName || '', res.avatarURL || '')
        navigate('/digital-twin')
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
      await authApi.sendEmailCode(identifier) as unknown
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
      setError(err instanceof Error ? err.message : '发送验证码失败')
    } finally {
      setCodeSending(false)
    }
  }

  const handleSocialLogin = async (provider: 'GITHUB' | 'WECHAT') => {
    // 微信登录暂未开发完成，显示提示
    if (provider === 'WECHAT') {
      setError('微信登录暂未开放，敬请期待！')
      return
    }
    
    try {
      const res = await authApi.getGitHubAuthUrl() as unknown as string
      if (res) window.location.href = res
    } catch (err) {
      setError('GitHub登录失败，请稍后重试')
    }
  }

  return (
    <div className="min-h-screen flex">
      {/* 左侧装饰区域 */}
      <div className="hidden lg:flex lg:w-1/2 relative bg-gradient-to-br from-water to-primary-600 overflow-hidden">
        <div className="absolute inset-0">
          <div className="absolute top-20 left-20 w-72 h-72 bg-white/10 rounded-full blur-3xl"></div>
          <div className="absolute bottom-20 right-20 w-96 h-96 bg-white/10 rounded-full blur-3xl"></div>
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-80 h-80 border border-white/20 rounded-full"></div>
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 border border-white/10 rounded-full"></div>
        </div>
        <div className="relative z-10 flex flex-col justify-center items-center w-full text-white p-12">
          <motion.div 
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ duration: 0.6 }}
            className="w-32 h-32 bg-white/20 backdrop-blur-sm rounded-3xl flex items-center justify-center mb-8"
          >
            <Droplets className="w-16 h-16" />
          </motion.div>
          <motion.h1 
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="text-4xl font-bold text-center mb-4"
          >
            校园水务数字孪生平台
          </motion.h1>
          <motion.p 
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.3 }}
            className="text-xl text-white/80 text-center"
          >
            智慧用水监测与决策平台
          </motion.p>
          <motion.div 
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.4 }}
            className="mt-12 flex gap-8 text-white/60"
          >
            <div className="text-center">
              <div className="text-2xl font-bold text-white">24h</div>
              <div className="text-sm">实时监测</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-white">AI</div>
              <div className="text-sm">智能分析</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-white">3D</div>
              <div className="text-sm">数字孪生</div>
            </div>
          </motion.div>
        </div>
      </div>

      {/* 右侧登录表单 */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8 bg-gray-50">
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="w-full max-w-md"
        >
          <div className="bg-white rounded-3xl shadow-xl p-8 border border-gray-100">
            <div className="lg:hidden text-center mb-8">
              <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-water to-primary-600 rounded-2xl mb-4">
                <Droplets className="w-8 h-8 text-white" />
              </div>
              <h1 className="text-2xl font-bold text-gray-900">校园水务数字孪生平台</h1>
            </div>

            <h2 className="text-2xl font-bold text-gray-900 mb-2">欢迎回来</h2>
            <p className="text-gray-500 mb-8">请登录您的账号</p>

            {/* 登录方式切换 */}
            <div className="flex rounded-2xl bg-gray-100 p-1.5 mb-6">
              {([
                { key: 'password', label: '密码登录' },
                { key: 'email', label: '邮箱登录' },
                { key: 'phone', label: '手机登录' }
              ] as const).map(({ key, label }) => (
                <button
                  key={key}
                  type="button"
                  onClick={() => setLoginMethod(key)}
                  className={`flex-1 py-2.5 px-3 rounded-xl text-sm font-medium transition-all duration-200 ${
                    loginMethod === key
                      ? 'bg-white text-primary-600 shadow-md'
                      : 'text-gray-500 hover:text-gray-700'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>

            <form onSubmit={handleSubmit} className="space-y-5">
              {/* 账号输入 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  {loginMethod === 'phone' ? '手机号' : loginMethod === 'email' ? '邮箱' : '账号'}
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
                    {loginMethod === 'phone' ? <Phone className="w-5 h-5" /> : <Mail className="w-5 h-5" />}
                  </div>
                  <input
                    type="text"
                    value={identifier}
                    onChange={(e) => setIdentifier(e.target.value)}
                    placeholder={loginMethod === 'phone' ? '请输入手机号' : loginMethod === 'email' ? '请输入邮箱' : '请输入Uid'}
                    className="w-full pl-12 pr-4 py-3.5 bg-gray-50 border-2 border-gray-100 rounded-xl focus:bg-white focus:border-primary-500 focus:outline-none transition-all text-gray-800 placeholder:text-gray-400"
                  />
                </div>
              </div>

              {/* 验证码或密码输入 */}
              {loginMethod === 'email' ? (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">验证码</label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
                      <MessageSquare className="w-5 h-5" />
                    </div>
                    <input
                      type="text"
                      value={code}
                      onChange={(e) => setCode(e.target.value)}
                      placeholder="请输入邮箱验证码"
                      className="w-full pl-12 pr-36 py-3.5 bg-gray-50 border-2 border-gray-100 rounded-xl focus:bg-white focus:border-primary-500 focus:outline-none transition-all text-gray-800 placeholder:text-gray-400"
                    />
                    <button
                      type="button"
                      onClick={handleSendCode}
                      disabled={codeSending || countdown > 0 || !identifier.includes('@')}
                      className="absolute right-2 top-1/2 -translate-y-1/2 px-4 py-2 text-sm font-medium rounded-lg transition-all disabled:text-gray-400 disabled:cursor-not-allowed bg-primary-50 text-primary-600 hover:bg-primary-100"
                    >
                      {codeSending ? '发送中...' : countdown > 0 ? `${countdown}s` : '获取验证码'}
                    </button>
                  </div>
                </div>
              ) : (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">密码</label>
                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
                      <Lock className="w-5 h-5" />
                    </div>
                    <input
                      type={showPassword ? 'text' : 'password'}
                      value={credential}
                      onChange={(e) => setCredential(e.target.value)}
                      placeholder="请输入密码"
                      className="w-full pl-12 pr-14 py-3.5 bg-gray-50 border-2 border-gray-100 rounded-xl focus:bg-white focus:border-primary-500 focus:outline-none transition-all text-gray-800 placeholder:text-gray-400"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute inset-y-0 right-0 pr-4 flex items-center text-gray-400 hover:text-gray-600 transition-colors"
                    >
                      {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                    </button>
                  </div>
                </div>
              )}

              {/* 错误提示 */}
              <AnimatePresence>
                {error && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    exit={{ opacity: 0, height: 0 }}
                    className="text-red-500 text-sm bg-red-50 px-4 py-3 rounded-xl"
                  >
                    {error}
                  </motion.div>
                )}
              </AnimatePresence>

              {/* 登录按钮 */}
              <button
                type="submit"
                disabled={loading}
                className="w-full py-3.5 px-4 bg-gradient-to-r from-water to-primary-600 hover:from-water/90 hover:to-primary-700 text-white font-semibold rounded-xl transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-lg shadow-primary-500/25"
              >
                {loading ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    登录中...
                  </>
                ) : (
                  '登录'
                )}
              </button>
            </form>

            {/* 注册链接 */}
            <div className="mt-8 text-center">
              <span className="text-gray-500">还没有账号？</span>
              <Link to="/register" className="text-primary-600 hover:text-primary-700 font-semibold ml-1.5 transition-colors">
                立即注册
              </Link>
            </div>

            {/* 第三方登录 */}
            <div className="mt-8 pt-6 border-t border-gray-100">
              <p className="text-center text-sm text-gray-400 mb-4">其他登录方式</p>
              <div className="flex justify-center gap-3">
                <button
                  type="button"
                  onClick={() => handleSocialLogin('GITHUB')}
                  className="flex items-center gap-2 px-5 py-2.5 border-2 border-gray-100 rounded-xl hover:bg-gray-50 hover:border-gray-200 transition-all group"
                >
                  <svg className="w-5 h-5 text-gray-600 group-hover:text-gray-900" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
                  </svg>
                  <span className="text-sm font-medium text-gray-600 group-hover:text-gray-900">GitHub</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleSocialLogin('WECHAT')}
                  className="flex items-center gap-2 px-5 py-2.5 border-2 border-gray-100 rounded-xl hover:bg-gray-50 hover:border-gray-200 transition-all group relative"
                  title="暂未开放"
                >
                  <svg className="w-5 h-5 text-gray-400 group-hover:text-gray-600" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.045c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.327-1.233a.582.582 0 0 1-.023-.156.49.49 0 0 1 .201-.398C23.024 18.48 24 16.82 24 14.98c0-3.21-2.931-5.837-6.656-6.088V8.89c-.135-.007-.264-.03-.406-.03zm-2.53 3.274c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.969-.982z"/>
                  </svg>
                  <span className="text-sm font-medium text-gray-400 group-hover:text-gray-600">微信</span>
                  <span className="absolute -top-1 -right-1 w-2 h-2 bg-orange-500 rounded-full"></span>
                </button>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  )
}

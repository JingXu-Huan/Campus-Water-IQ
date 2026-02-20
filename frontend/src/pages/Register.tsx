import { useState, useMemo } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authApi, SignUpResult } from '@/api/auth'
import { Eye, EyeOff, Droplets, Mail, Phone, Lock, User, MessageSquare, Loader2, Check, X, ArrowRight } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import * as React from "react";

type RegisterMethod = 'email' | 'phone'

// 密码强度检查
const getPasswordStrength = (password: string): { level: number; label: string; color: string } => {
  let strength = 0
  if (password.length >= 6) strength++
  if (password.length >= 8) strength++
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) strength++
  if (/\d/.test(password)) strength++
  if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) strength++

  if (strength <= 1) return { level: 1, label: '太弱', color: 'bg-red-500' }
  if (strength <= 2) return { level: 2, label: '较弱', color: 'bg-orange-500' }
  if (strength <= 3) return { level: 3, label: '中等', color: 'bg-yellow-500' }
  if (strength <= 4) return { level: 4, label: '较强', color: 'bg-green-500' }
  return { level: 5, label: '很强', color: 'bg-green-600' }
}

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

  const passwordStrength = useMemo(() => getPasswordStrength(credential), [credential])
  const passwordMatch = useMemo(() => 
    credential && confirmPwd ? credential === confirmPwd : null
  , [credential, confirmPwd])

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
      await authApi.sendSignUpEmailCode(identifier) as unknown
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
    if (nickname.length < 2) {
      setError('昵称至少2个字符')
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
      const res = await authApi.signUp({
        type: registerMethod === 'email' ? 'MAIL_AND_CODE' : 'PHONE_AND_CODE',
        identifier,
        credential: code,
        pwd: credential,
        nickname
      }) as unknown as SignUpResult
      
      if (res.isSuccess) {
        setSuccess('注册成功！正在跳转...')
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
    <div className="min-h-screen flex">
      {/* 左侧装饰区域 */}
      <div className="hidden lg:flex lg:w-1/2 relative bg-gradient-to-br from-primary-600 to-water overflow-hidden">
        <div className="absolute inset-0">
          <div className="absolute top-20 right-20 w-72 h-72 bg-white/10 rounded-full blur-3xl"></div>
          <div className="absolute bottom-20 left-20 w-96 h-96 bg-white/10 rounded-full blur-3xl"></div>
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-80 h-80 border border-white/20 rounded-full"></div>
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
            加入我们
          </motion.h1>
          <motion.p 
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.3 }}
            className="text-xl text-white/80 text-center max-w-md"
          >
            创建账号，体验智慧水务管理
          </motion.p>
          <motion.div 
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.4 }}
            className="mt-12 space-y-4 text-left"
          >
            {[
              '实时监测用水数据',
              'AI智能分析预测',
              '3D数字孪生可视化',
              '异常报警及时推送'
            ].map((item, i) => (
              <motion.div 
                key={i}
                initial={{ x: -20, opacity: 0 }}
                animate={{ x: 0, opacity: 1 }}
                transition={{ duration: 0.3, delay: 0.5 + i * 0.1 }}
                className="flex items-center gap-3"
              >
                <div className="w-6 h-6 rounded-full bg-white/20 flex items-center justify-center">
                  <Check className="w-4 h-4" />
                </div>
                <span>{item}</span>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </div>

      {/* 右侧注册表单 */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8 bg-gray-50">
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="w-full max-w-md"
        >
          <div className="bg-white rounded-3xl shadow-xl p-8 border border-gray-100">
            <div className="lg:hidden text-center mb-8">
              <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-primary-600 to-water rounded-2xl mb-4">
                <Droplets className="w-8 h-8 text-white" />
              </div>
              <h1 className="text-2xl font-bold text-gray-900">校园水务数字孪生平台</h1>
            </div>

            <h2 className="text-2xl font-bold text-gray-900 mb-2">创建账号</h2>
            <p className="text-gray-500 mb-8">填写以下信息完成注册</p>

            {/* 注册方式切换 */}
            <div className="flex rounded-2xl bg-gray-100 p-1.5 mb-6">
              {([
                { key: 'email', label: '邮箱注册' },
                { key: 'phone', label: '手机注册' }
              ] as const).map(({ key, label }) => (
                <button
                  key={key}
                  type="button"
                  onClick={() => setRegisterMethod(key)}
                  className={`flex-1 py-2.5 px-3 rounded-xl text-sm font-medium transition-all duration-200 ${
                    registerMethod === key
                      ? 'bg-white text-primary-600 shadow-md'
                      : 'text-gray-500 hover:text-gray-700'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              {/* 昵称 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">昵称</label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
                    <User className="w-5 h-5" />
                  </div>
                  <input
                    type="text"
                    value={nickname}
                    onChange={(e) => setNickname(e.target.value)}
                    placeholder="请输入昵称"
                    className="w-full pl-12 pr-4 py-3.5 bg-gray-50 border-2 border-gray-100 rounded-xl focus:bg-white focus:border-primary-500 focus:outline-none transition-all text-gray-800 placeholder:text-gray-400"
                  />
                </div>
              </div>

              {/* 邮箱/手机 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  {registerMethod === 'email' ? '邮箱' : '手机号'}
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
                    {registerMethod === 'email' ? <Mail className="w-5 h-5" /> : <Phone className="w-5 h-5" />}
                  </div>
                  <input
                    type="text"
                    value={identifier}
                    onChange={(e) => setIdentifier(e.target.value)}
                    placeholder={registerMethod === 'email' ? '请输入邮箱' : '请输入手机号'}
                    className="w-full pl-12 pr-4 py-3.5 bg-gray-50 border-2 border-gray-100 rounded-xl focus:bg-white focus:border-primary-500 focus:outline-none transition-all text-gray-800 placeholder:text-gray-400"
                  />
                </div>
              </div>

              {/* 验证码 */}
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
                    placeholder="请输入验证码"
                    className="w-full pl-12 pr-36 py-3.5 bg-gray-50 border-2 border-gray-100 rounded-xl focus:bg-white focus:border-primary-500 focus:outline-none transition-all text-gray-800 placeholder:text-gray-400"
                  />
                  <button
                    type="button"
                    onClick={sendCode}
                    disabled={countdown > 0 || sendingCode || (registerMethod === 'email' && !identifier.includes('@'))}
                    className="absolute right-2 top-1/2 -translate-y-1/2 px-4 py-2 text-sm font-medium rounded-lg transition-all disabled:text-gray-400 disabled:cursor-not-allowed bg-primary-50 text-primary-600 hover:bg-primary-100"
                  >
                    {countdown > 0 ? `${countdown}s` : sendingCode ? '发送中...' : '获取验证码'}
                  </button>
                </div>
              </div>

              {/* 密码 */}
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
                    placeholder="请输入密码（至少6位）"
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
                {/* 密码强度指示器 */}
                {credential && (
                  <div className="mt-2">
                    <div className="flex gap-1.5 mb-1">
                      {[1, 2, 3, 4, 5].map((level) => (
                        <div
                          key={level}
                          className={`h-1.5 flex-1 rounded-full transition-colors ${
                            level <= passwordStrength.level ? passwordStrength.color : 'bg-gray-200'
                          }`}
                        />
                      ))}
                    </div>
                    <div className="flex justify-between text-xs">
                      <span className={passwordStrength.color.replace('bg-', 'text-')}>{passwordStrength.label}</span>
                      <span className="text-gray-400">建议使用8位以上包含大小写字母、数字和符号的密码</span>
                    </div>
                  </div>
                )}
              </div>

              {/* 确认密码 */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">确认密码</label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-gray-400">
                    <Lock className="w-5 h-5" />
                  </div>
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={confirmPwd}
                    onChange={(e) => setConfirmPwd(e.target.value)}
                    placeholder="请再次输入密码"
                    className={`w-full pl-12 pr-12 py-3.5 bg-gray-50 border-2 rounded-xl focus:bg-white focus:outline-none transition-all text-gray-800 placeholder:text-gray-400 ${
                      passwordMatch === true 
                        ? 'border-green-500 focus:border-green-500' 
                        : passwordMatch === false 
                          ? 'border-red-500 focus:border-red-500'
                          : 'border-gray-100 focus:border-primary-500'
                    }`}
                  />
                  <div className="absolute inset-y-0 right-0 pr-4 flex items-center">
                    {passwordMatch === true && <Check className="w-5 h-5 text-green-500" />}
                    {passwordMatch === false && <X className="w-5 h-5 text-red-500" />}
                  </div>
                </div>
              </div>

              {/* 错误/成功提示 */}
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
                {success && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    exit={{ opacity: 0, height: 0 }}
                    className="text-green-500 text-sm bg-green-50 px-4 py-3 rounded-xl flex items-center gap-2"
                  >
                    <Check className="w-4 h-4" />
                    {success}
                  </motion.div>
                )}
              </AnimatePresence>

              {/* 注册按钮 */}
              <button
                type="submit"
                disabled={loading}
                className="w-full py-3.5 px-4 bg-gradient-to-r from-water to-primary-600 hover:from-water/90 hover:to-primary-700 text-white font-semibold rounded-xl transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-lg shadow-primary-500/25"
              >
                {loading ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    注册中...
                  </>
                ) : (
                  <>
                    注册
                    <ArrowRight className="w-5 h-5" />
                  </>
                )}
              </button>
            </form>

            {/* 登录链接 */}
            <div className="mt-8 text-center">
              <span className="text-gray-500">已有账号？</span>
              <Link to="/login" className="text-primary-600 hover:text-primary-700 font-semibold ml-1.5 transition-colors">
                立即登录
              </Link>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  )
}

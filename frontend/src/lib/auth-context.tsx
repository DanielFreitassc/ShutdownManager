"use client"

import type React from "react"
import {
  createContext,
  useContext,
  useState,
  useEffect,
  useMemo,
} from "react"
import { useRouter } from "next/navigation"
import {
  type User,
  type LoginPayload,
  type RegisterPayload,
  type AuthResponse,
  login as apiLogin,
  register as apiRegister,
  getMe,
} from "@/lib/api"
import { getToken, setToken, removeToken } from "@/lib/auth"

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  loading: boolean
  login: (payload: LoginPayload) => Promise<void>
  register: (payload: RegisterPayload) => Promise<AuthResponse>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [loading, setLoading] = useState(true)
  const router = useRouter()

  // ✅ Valida o token no carregamento inicial
  useEffect(() => {
    const validateToken = async () => {
      const token = getToken()
      if (token) {
        try {
          const userData = await getMe()
          setUser(userData)
          setIsAuthenticated(true)
        } catch (error) {
          removeToken()
          setUser(null)
          setIsAuthenticated(false)
          console.error("Falha ao validar token:", error)
        }
      } else {
        setUser(null)
        setIsAuthenticated(false)
      }
      setLoading(false)
    }

    validateToken()
  }, [])

  // ✅ Login
  const login = async (payload: LoginPayload) => {
    try {
      const { token, user } = await apiLogin(payload)
      if (!token) throw new Error("Token ausente na resposta do servidor")

      setToken(token)
      setUser(user ?? (await getMe()))
      setIsAuthenticated(true)
    } catch (error) {
      console.error("[AuthContext] Login error:", error)
      throw error
    }
  }

  // ✅ Registro agora retorna AuthResponse completo
  const register = async (payload: RegisterPayload): Promise<AuthResponse> => {
    try {
      const response = await apiRegister(payload)

      if (response.token) {
        setToken(response.token)
        setIsAuthenticated(true)
      } else {
        setIsAuthenticated(false)
      }

      if (response.user) {
        setUser(response.user)
      }

      return response
    } catch (error) {
      console.error("[AuthContext] Register error:", error)
      throw error
    }
  }

  // ✅ Logout
  const logout = () => {
    removeToken()
    setUser(null)
    setIsAuthenticated(false)
    router.push("/login")
  }

  const value = useMemo(
    () => ({
      user,
      isAuthenticated,
      loading,
      login,
      register,
      logout,
    }),
    [user, isAuthenticated, loading],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// ✅ Hook customizado
export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error("useAuth deve ser usado dentro de um AuthProvider")
  }
  return context
}

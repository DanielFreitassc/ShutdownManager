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
  login as apiLogin,
  register as apiRegister,
  getMe,
} from "@/lib/api"
import { getToken, setToken, removeToken } from "@/lib/auth"

// Define a forma do contexto
interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  loading: boolean
  login: (payload: LoginPayload) => Promise<void>
  register: (payload: RegisterPayload) => Promise<void>
  logout: () => void
}

// Cria o contexto com um valor padrão
const AuthContext = createContext<AuthContextType | undefined>(undefined)

// Cria o Provedor
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [loading, setLoading] = useState(true)
  const router = useRouter()

  // Efeito para validar o token no carregamento inicial
  useEffect(() => {
    const validateToken = async () => {
      const token = getToken()
      if (token) {
        try {
          // Valida o token buscando os dados do usuário
          const userData = await getMe()
          setUser(userData)
          setIsAuthenticated(true)
        } catch (error) {
          // Token inválido ou expirado
          removeToken()
          setUser(null)
          setIsAuthenticated(false)
          console.error("Falha ao validar token:", error)
        }
      } else {
        setIsAuthenticated(false)
        setUser(null)
      }
      setLoading(false)
    }

    validateToken()
  }, [])

  // Função de Login
  const login = async (payload: LoginPayload) => {
    try {
      // CORRIGIDO: Removido o erro de sintaxe `_`
      const { token, user } = await apiLogin(payload)
      setToken(token)
      // Se a API de login não retornar o usuário, podemos buscá-lo com getMe
      if (user) {
        setUser(user)
      } else {
        const userData = await getMe() // Busca dados do usuário após logar
        setUser(userData)
      }
      setIsAuthenticated(true)
      // O redirecionamento será tratado pela página de login
    } catch (error) {
      console.error("[AuthContext] Login error:", error)
      // Propaga o erro para o formulário de login tratar
      throw error
    }
  }

  // Função de Registro
  const register = async (payload: RegisterPayload) => {
    try {
      const { token, user } = await apiRegister(payload)
      setToken(token)
      if (user) {
        setUser(user)
      } else {
        const userData = await getMe()
        setUser(userData)
      }
      setIsAuthenticated(true)
      // O redirecionamento será tratado pela página de registro
    } catch (error) {
      console.error("[AuthContext] Register error:", error)
      throw error
    }
  }

  // Função de Logout
  const logout = () => {
    removeToken()
    setUser(null)
    setIsAuthenticated(false)
    // Força o redirecionamento para a página de login
    router.push("/login")
  }

  // Otimiza o valor do contexto com useMemo
  const value = useMemo(
    () => ({
      user,
      isAuthenticated,
      loading,
      login,
      register,
      logout,
    }),
    // CORRIGIDO: Adicionadas `login`, `register`, `logout` ao array de dependências
    [user, isAuthenticated, loading, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// Hook customizado para consumir o contexto
export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error("useAuth deve ser usado dentro de um AuthProvider")
  }
  return context
}
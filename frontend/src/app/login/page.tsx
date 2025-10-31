"use client"

import type React from "react"
import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { useAuth } from "@/lib/auth-context"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { toast } from "react-toastify"
import { Loader2 } from "lucide-react"

export default function LoginPage() {
  const router = useRouter()
  // Pega loading do contexto como authLoading para evitar conflito
  const { login, isAuthenticated, loading: authLoading } = useAuth()
  // Loading local para o formulário
  const [localLoading, setLocalLoading] = useState(false)
  const [formData, setFormData] = useState({
    email: "",
    password: "",
  })

  // CORRIGIDO: Efeito que redireciona *após* o estado de autenticação ser atualizado
  useEffect(() => {
    // Se a autenticação não estiver carregando e o usuário estiver autenticado
    if (!authLoading && isAuthenticated) {
      router.push("/dashboard")
    }
  }, [isAuthenticated, authLoading, router])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLocalLoading(true)

    try {
      await login(formData)
      toast.success("Login realizado com sucesso! Redirecionando...")
      // Não é mais necessário redirecionar manualmente aqui.
      // O useEffect acima cuidará disso quando o estado `isAuthenticated` mudar.
    } catch (error: any) {
      console.error("[Login Page] Login error:", error)
      // CORRIGIDO: Agora `error.message` contém a mensagem da API (graças ao api.ts)
      toast.error(
        error.message || "Erro ao fazer login. Verifique suas credenciais.",
      )
      setLocalLoading(false)
    }
    // Não definimos localLoading(false) em caso de sucesso,
    // pois a página será redirecionada.
  }

  // O loading total considera o carregamento do contexto e o submit do form
  const isLoading = localLoading || authLoading

  // Se já estiver logado (ex: acessou /login manualmente), o useEffect redirecionará
  if (authLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loader2 className="h-12 w-12 animate-spin text-primary" />
      </div>
    )
  }

  // Não renderiza o formulário se já estiver autenticado (e esperando redirect)
  if (!authLoading && isAuthenticated) {
     return (
      <div className="flex items-center justify-center min-h-screen">
        <Loader2 className="h-12 w-12 animate-spin text-primary" />
        <p className="ml-4">Redirecionando...</p>
      </div>
    )
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-muted/30">
      <Card className="w-full max-w-md mx-4">
        <CardHeader className="space-y-1">
          <CardTitle className="text-2xl font-bold text-center">
            Machine Manager
          </CardTitle>
          <CardDescription className="text-center">
            Entre com suas credenciais para acessar o sistema
          </CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit}>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                placeholder="admin@admin.com"
                value={formData.email}
                onChange={(e) =>
                  setFormData({ ...formData, email: e.target.value })
                }
                required
                disabled={isLoading}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">Senha</Label>
              <Input
                id="password"
                type="password"
                placeholder="••••••••"
                value={formData.password}
                onChange={(e) =>
                  setFormData({ ...formData, password: e.target.value })
                }
                required
                disabled={isLoading}
              />
            </div>
          </CardContent>
          <CardFooter className="flex flex-col space-y-4">
            <Button type="submit" className="w-full" disabled={isLoading}>
              {isLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Entrando...
                </>
              ) : (
                "Entrar"
              )}
            </Button>
            <p className="text-sm text-center text-muted-foreground">
              Não tem uma conta?{" "}
              <Link
                href="/register"
                className="text-primary hover:underline font-medium"
              >
                Registre-se
              </Link>
            </p>
          </CardFooter>
        </form>
      </Card>
    </div>
  )
}

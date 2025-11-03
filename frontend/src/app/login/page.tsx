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
  const { login, isAuthenticated, loading: authLoading } = useAuth()
  const [localLoading, setLocalLoading] = useState(false)
  const [formData, setFormData] = useState({ email: "", password: "" })
  const isLoading = localLoading || authLoading

  useEffect(() => {
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
    } catch (error: any) {
      console.error("[Login Page] Login error:", error)
      toast.error(error.message || "Erro ao fazer login. Verifique suas credenciais.")
      setLocalLoading(false)
    }
  }

  if (authLoading || (isAuthenticated && !authLoading)) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen space-y-4">
        <Loader2 className="h-12 w-12 animate-spin text-primary" />
        <p>Redirecionando...</p>
      </div>
    )
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-muted/30 px-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-2 py-6">
          <CardTitle className="text-2xl font-bold text-center">
            Machine Manager
          </CardTitle>
          <CardDescription className="text-center text-muted-foreground">
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

          <CardFooter className="flex flex-col space-y-3 py-4">
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
                className="text-primary font-medium hover:underline"
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

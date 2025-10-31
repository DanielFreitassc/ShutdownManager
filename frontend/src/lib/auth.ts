import Cookies from "js-cookie"

const TOKEN_KEY = "auth_token"

// Salva o token no Cookie.
export function setToken(token: string) {
  Cookies.set(TOKEN_KEY, token, {
    expires: 7, // 7 dias
    secure: process.env.NODE_ENV === "production",
    sameSite: "Lax",
    path: "/",
  })
}

// Obtém o token do Cookie.
export function getToken(): string | undefined {
  return Cookies.get(TOKEN_KEY)
}

// Remove o token.
export function removeToken() {
  Cookies.remove(TOKEN_KEY, { path: "/" })
}

// Verifica se o token existe.
export function isAuthenticated(): boolean {
  return !!getToken()
}

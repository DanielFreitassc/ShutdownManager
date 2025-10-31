export interface User {
  id: string
  name: string
  email: string
  role: "ADMIN" | "USER"
  createdAt: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
  user?: User
}

export interface RegisterRequest {
  name: string
  email: string
  password: string
}

export interface RegisterResponse {
  message: string
}

export interface PaginatedResponse<T> {
  content: T[]
  pageable: {
    pageNumber: number
    pageSize: number
    sort: {
      sorted: boolean
      unsorted: boolean
      empty: boolean
    }
    offset: number
    paged: boolean
    unpaged: boolean
  }
  totalPages: number
  totalElements: number
  last: boolean
  size: number
  number: number
  sort: {
    sorted: boolean
    unsorted: boolean
    empty: boolean
  }
  first: boolean
  numberOfElements: number
  empty: boolean
}

export interface Agent {
  id: string
  group: string
  hostname: string
  key: string
  status: "online" | "offline" | "new"
  lastHeartbeat: string
}

export interface CommandRequest {
  command: string
}

export interface CommandGroupRequest {
  group: string
  command: string
}

export interface CommandHostnameRequest {
  hostname: string
  command: string
}

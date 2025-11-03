"use client"

import { useState, useEffect } from "react"
import { api } from "@/lib/api"
import type { User, PaginatedResponse } from "@/types"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { toast } from "react-toastify"
import {
  Loader2,
  CheckCircle,
  Trash2,
  ChevronLeft,
  ChevronRight,
  UserCheck,
  Users,
  UserCog,
  ShieldAlert,
} from "lucide-react"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Label } from "@/components/ui/label"
import { Input } from "@/components/ui/input"

// Estado inicial para o formulário de edição
const initialEditFormState = {
  name: "",
  email: "",
  password: "",
}

export default function UsersPage() {
  const [pendingUsers, setPendingUsers] = useState<User[]>([])
  const [activeUsers, setActiveUsers] = useState<User[]>([])

  const [loadingPending, setLoadingPending] = useState(true)
  const [loadingActive, setLoadingActive] = useState(true)

  const [pendingPage, setPendingPage] = useState(0)
  const [activePage, setActivePage] = useState(0)

  const [pendingTotalPages, setPendingTotalPages] = useState(0)
  const [activeTotalPages, setActiveTotalPages] = useState(0)

  const [activatingId, setActivatingId] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [updatingId, setUpdatingId] = useState<string | null>(null)

  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [userToDelete, setUserToDelete] = useState<User | null>(null)

  const [editDialogOpen, setEditDialogOpen] = useState(false)
  const [userToEdit, setUserToEdit] = useState<User | null>(null)
  const [editFormData, setEditFormData] = useState(initialEditFormState)

  const pageSize = 10

  // Busca usuários pendentes
  const fetchPendingUsers = async (page: number) => {
    setLoadingPending(true)
    try {
      const response = await api.get<PaginatedResponse<User>>(
        `/users/pending?page=${page}&size=${pageSize}`,
      )
      setPendingUsers(response.data.content)
      setPendingTotalPages(response.data.totalPages)
    } catch (error: any) {
      console.error("[v0] Error fetching pending users:", error)
      toast.error(
        error.response?.data?.message ||
          "Não foi possível carregar os usuários pendentes.",
      )
    } finally {
      setLoadingPending(false)
    }
  }

  // Busca usuários ativos
  const fetchActiveUsers = async (page: number) => {
    setLoadingActive(true)
    try {
      const response = await api.get<PaginatedResponse<User>>(
        `/users?page=${page}&size=${pageSize}`,
      )
      setActiveUsers(response.data.content)
      setActiveTotalPages(response.data.totalPages)
    } catch (error: any) {
      console.error("[v0] Error fetching active users:", error)
      toast.error(
        error.response?.data?.message ||
          "Não foi possível carregar os usuários ativos.",
      )
    } finally {
      setLoadingActive(false)
    }
  }

  // Efeitos para carregar dados
  useEffect(() => {
    fetchPendingUsers(pendingPage)
  }, [pendingPage])

  useEffect(() => {
    fetchActiveUsers(activePage)
  }, [activePage])

  // Recarrega ambas as listas
  const refreshLists = () => {
    fetchPendingUsers(pendingPage)
    fetchActiveUsers(activePage)
  }

  // Formata a data
  const formatDate = (dateString: string) => {
    try {
      const date = new Date(dateString)
      return date.toLocaleString("pt-BR")
    } catch {
      return dateString
    }
  }

  // --- Funções de Ação ---

  const handleActivateToggle = async (userId: string) => {
    setActivatingId(userId)
    try {
      await api.post(`/users/${userId}/activate`)
      toast.success("Status do usuário alterado com sucesso.")
      refreshLists() // Recarrega ambas as listas
    } catch (error: any) {
      console.error("[v0] Error activating/deactivating user:", error)
      toast.error(
        error.response?.data?.message || "Não foi possível alterar o status.",
      )
    } finally {
      setActivatingId(null)
    }
  }

  const handleDeleteClick = (user: User) => {
    setUserToDelete(user)
    setDeleteDialogOpen(true)
  }

  const handleDeleteConfirm = async () => {
    if (!userToDelete) return

    setDeletingId(userToDelete.id)
    try {
      await api.delete(`/users/${userToDelete.id}`)
      toast.success("Usuário excluído com sucesso.")
      refreshLists() // Recarrega ambas as listas
    } catch (error: any) {
      console.error("[v0] Error deleting user:", error)
      toast.error(
        error.response?.data?.message || "Não foi possível excluir o usuário.",
      )
    } finally {
      setDeletingId(null)
      setDeleteDialogOpen(false)
      setUserToDelete(null)
    }
  }

  const openEditDialog = (user: User) => {
    setUserToEdit(user)
    setEditFormData({
      name: user.name,
      email: user.email,
      password: "", // Senha sempre começa em branco por segurança
    })
    setEditDialogOpen(true)
  }

  const handleUpdateUser = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!userToEdit) return

    setUpdatingId(userToEdit.id)

    // Monta o payload apenas com dados alterados
    const payload: { name?: string; email?: string; password?: string } = {}
    if (editFormData.name !== userToEdit.name) {
      payload.name = editFormData.name
    }
    if (editFormData.email !== userToEdit.email) {
      payload.email = editFormData.email
    }
    if (editFormData.password) {
      payload.password = editFormData.password
    }

    if (Object.keys(payload).length === 0) {
      toast.info("Nenhum dado foi alterado.")
      setUpdatingId(null)
      setEditDialogOpen(false)
      return
    }

    try {
      await api.patch(`/users/${userToEdit.id}`, payload)
      toast.success("Usuário atualizado com sucesso.")
      refreshLists() // Recarrega ambas as listas
      setEditDialogOpen(false)
      setUserToEdit(null)
    } catch (error: any) {
      console.error("[v0] Error updating user:", error)
      toast.error(
        error.response?.data?.message || "Não foi possível atualizar o usuário.",
      )
    } finally {
      setUpdatingId(null)
    }
  }

  // --- Renderização ---

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">
          Gerenciamento de Usuários
        </h1>
        <p className="text-muted-foreground mt-2">
          Gerencie usuários pendentes, ativos e edite contas.
        </p>
      </div>

      <Tabs defaultValue="pending" className="space-y-4">
        <TabsList>
          <TabsTrigger value="pending" className="gap-2">
            <UserCheck className="h-4 w-4" />
            Usuários Pendentes
          </TabsTrigger>
          <TabsTrigger value="active" className="gap-2">
            <Users className="h-4 w-4" />
            Usuários Ativos
          </TabsTrigger>
        </TabsList>

        {/* ABA DE USUÁRIOS PENDENTES */}
        <TabsContent value="pending" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Usuários Aguardando Aprovação</CardTitle>
              <CardDescription>
                Ative ou rejeite usuários que solicitaram acesso
              </CardDescription>
            </CardHeader>
            <CardContent>
              {loadingPending ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                </div>
              ) : pendingUsers.length === 0 ? (
                <div className="text-center py-8">
                  <CheckCircle className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                  <p className="text-muted-foreground">
                    Nenhum usuário pendente no momento
                  </p>
                </div>
              ) : (
                <>
                  <div className="rounded-md border">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Nome</TableHead>
                          <TableHead>Email</TableHead>
                          <TableHead>Função</TableHead>
                          <TableHead>Data de Criação</TableHead>
                          <TableHead className="text-right">Ações</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {pendingUsers.map((user) => (
                          <TableRow key={user.id}>
                            <TableCell className="font-medium">
                              {user.name}
                            </TableCell>
                            <TableCell>{user.email}</TableCell>
                            <TableCell>
                              <Badge variant="outline">{user.role}</Badge>
                            </TableCell>
                            <TableCell>{formatDate(user.createdAt)}</TableCell>
                            <TableCell className="text-right">
                              <div className="flex justify-end gap-2">
                                <Button
                                  size="sm"
                                  variant="ghost"
                                  onClick={() => openEditDialog(user)}
                                  disabled={
                                    activatingId === user.id ||
                                    deletingId === user.id
                                  }
                                >
                                  <UserCog className="h-4 w-4" />
                                </Button>
                                <Button
                                  size="sm"
                                  onClick={() => handleActivateToggle(user.id)}
                                  disabled={
                                    activatingId === user.id ||
                                    deletingId === user.id
                                  }
                                >
                                  {activatingId === user.id ? (
                                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                                  ) : (
                                    <CheckCircle className="mr-2 h-4 w-4" />
                                  )}
                                  Ativar
                                </Button>
                                <Button
                                  size="sm"
                                  variant="destructive"
                                  onClick={() => handleDeleteClick(user)}
                                  disabled={
                                    activatingId === user.id ||
                                    deletingId === user.id
                                  }
                                >
                                  {deletingId === user.id ? (
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                  ) : (
                                    <Trash2 className="h-4 w-4" />
                                  )}
                                </Button>
                              </div>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                  {/* Paginação Pendentes */}
                  {pendingTotalPages > 1 && (
                    <div className="flex items-center justify-between mt-4">
                      <p className="text-sm text-muted-foreground">
                        Página {pendingPage + 1} de {pendingTotalPages}
                      </p>
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setPendingPage((p) => Math.max(0, p - 1))}
                          disabled={pendingPage === 0}
                        >
                          <ChevronLeft className="h-4 w-4 mr-1" />
                          Anterior
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() =>
                            setPendingPage((p) =>
                              Math.min(pendingTotalPages - 1, p + 1),
                            )
                          }
                          disabled={pendingPage >= pendingTotalPages - 1}
                        >
                          Próxima
                          <ChevronRight className="h-4 w-4 ml-1" />
                        </Button>
                      </div>
                    </div>
                  )}
                </>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* ABA DE USUÁRIOS ATIVOS */}
        <TabsContent value="active" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Usuários Ativos</CardTitle>
              <CardDescription>
                Gerencie usuários que já têm acesso ao sistema
              </CardDescription>
            </CardHeader>
            <CardContent>
              {loadingActive ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                </div>
              ) : activeUsers.length === 0 ? (
                <div className="text-center py-8">
                  <Users className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                  <p className="text-muted-foreground">
                    Nenhum usuário ativo encontrado
                  </p>
                </div>
              ) : (
                <>
                  <div className="rounded-md border">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Nome</TableHead>
                          <TableHead>Email</TableHead>
                          <TableHead>Função</TableHead>
                          <TableHead>Data de Criação</TableHead>
                          <TableHead className="text-right">Ações</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {activeUsers.map((user) => (
                          <TableRow key={user.id}>
                            <TableCell className="font-medium">
                              {user.name}
                            </TableCell>
                            <TableCell>{user.email}</TableCell>
                            <TableCell>
                              <Badge
                                variant={
                                  user.role === "ADMIN"
                                    ? "default"
                                    : "secondary"
                                }
                              >
                                {user.role}
                              </Badge>
                            </TableCell>
                            <TableCell>{formatDate(user.createdAt)}</TableCell>
                            <TableCell className="text-right">
                              <div className="flex justify-end gap-2">
                                <Button
                                  size="sm"
                                  variant="ghost"
                                  onClick={() => openEditDialog(user)}
                                  disabled={
                                    activatingId === user.id ||
                                    deletingId === user.id
                                  }
                                >
                                  <UserCog className="h-4 w-4" />
                                </Button>
                                <Button
                                  size="sm"
                                  variant="outline"
                                  onClick={() => handleActivateToggle(user.id)}
                                  disabled={
                                    activatingId === user.id ||
                                    deletingId === user.id
                                  }
                                >
                                  {activatingId === user.id ? (
                                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                                  ) : (
                                    <ShieldAlert className="mr-2 h-4 w-4" />
                                  )}
                                  Desativar
                                </Button>
                                <Button
                                  size="sm"
                                  variant="destructive"
                                  onClick={() => handleDeleteClick(user)}
                                  disabled={
                                    activatingId === user.id ||
                                    deletingId === user.id
                                  }
                                >
                                  {deletingId === user.id ? (
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                  ) : (
                                    <Trash2 className="h-4 w-4" />
                                  )}
                                </Button>
                              </div>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                  {/* Paginação Ativos */}
                  {activeTotalPages > 1 && (
                    <div className="flex items-center justify-between mt-4">
                      <p className="text-sm text-muted-foreground">
                        Página {activePage + 1} de {activeTotalPages}
                      </p>
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setActivePage((p) => Math.max(0, p - 1))}
                          disabled={activePage === 0}
                        >
                          <ChevronLeft className="h-4 w-4 mr-1" />
                          Anterior
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() =>
                            setActivePage((p) =>
                              Math.min(activeTotalPages - 1, p + 1),
                            )
                          }
                          disabled={activePage >= activeTotalPages - 1}
                        >
                          Próxima
                          <ChevronRight className="h-4 w-4 ml-1" />
                        </Button>
                      </div>
                    </div>
                  )}
                </>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* MODAL DE EDIÇÃO DE USUÁRIO */}
      <Dialog open={editDialogOpen} onOpenChange={setEditDialogOpen}>
        <DialogContent>
          <form onSubmit={handleUpdateUser}>
            <DialogHeader>
              <DialogTitle>Editar Usuário</DialogTitle>
              <DialogDescription>
                Atualize os dados de {userToEdit?.name}. Deixe a senha em branco
                para não alterá-la.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="name">Nome</Label>
                <Input
                  id="name"
                  value={editFormData.name}
                  onChange={(e) =>
                    setEditFormData({ ...editFormData, name: e.target.value })
                  }
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  value={editFormData.email}
                  onChange={(e) =>
                    setEditFormData({ ...editFormData, email: e.target.value })
                  }
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">Nova Senha (Opcional)</Label>
                <Input
                  id="password"
                  type="password"
                  placeholder="Deixe em branco para manter a senha atual"
                  value={editFormData.password}
                  onChange={(e) =>
                    setEditFormData({
                      ...editFormData,
                      password: e.target.value,
                    })
                  }
                />
              </div>
            </div>

            <DialogFooter>
              <Button
                variant="outline"
                type="button"
                onClick={() => setEditDialogOpen(false)}
                disabled={updatingId === userToEdit?.id}
              >
                Cancelar
              </Button>
              <Button
                type="submit"
                disabled={updatingId === userToEdit?.id}
              >
                {updatingId === userToEdit?.id ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Salvando...
                  </>
                ) : (
                  "Salvar Alterações"
                )}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* MODAL DE EXCLUSÃO (Já existente) */}
      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Confirmar exclusão</AlertDialogTitle>
            <AlertDialogDescription>
              Tem certeza que deseja excluir o usuário{" "}
              <strong>{userToDelete?.name}</strong>? Esta ação não pode ser
              desfeita.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteConfirm}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              Excluir
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
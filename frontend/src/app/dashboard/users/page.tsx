"use client"

import { useState, useEffect } from "react"
import { api } from "@/lib/api"
import type { User, PaginatedResponse } from "@/types"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { toast } from "react-toastify"
import { Loader2, CheckCircle, Trash2, ChevronLeft, ChevronRight, UserCheck } from "lucide-react"
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"

export default function UsersPage() {
  const [pendingUsers, setPendingUsers] = useState<User[]>([])
  const [loadingPending, setLoadingPending] = useState(true)
  const [pendingPage, setPendingPage] = useState(0)
  const [pendingTotalPages, setPendingTotalPages] = useState(0)
  const [activatingId, setActivatingId] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [userToDelete, setUserToDelete] = useState<User | null>(null)

  const pageSize = 10

  const fetchPendingUsers = async (page: number) => {
    setLoadingPending(true)
    try {
      const response = await api.get<PaginatedResponse<User>>(`/users/pending?page=${page}&size=${pageSize}`)
      setPendingUsers(response.data.content)
      setPendingTotalPages(response.data.totalPages)
    } catch (error: any) {
      console.error("[v0] Error fetching pending users:", error)
      toast.error(error.response?.data?.message || "Não foi possível carregar os usuários pendentes.")
    } finally {
      setLoadingPending(false)
    }
  }

  useEffect(() => {
    fetchPendingUsers(pendingPage)
  }, [pendingPage])

  const handleActivate = async (userId: string) => {
    setActivatingId(userId)
    try {
      await api.post(`/users/${userId}/activate`)
      toast.success("Usuário ativado com sucesso.")
      fetchPendingUsers(pendingPage)
    } catch (error: any) {
      console.error("[v0] Error activating user:", error)
      toast.error(error.response?.data?.message || "Não foi possível ativar o usuário.")
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
      fetchPendingUsers(pendingPage)
    } catch (error: any) {
      console.error("[v0] Error deleting user:", error)
      toast.error(error.response?.data?.message || "Não foi possível excluir o usuário.")
    } finally {
      setDeletingId(null)
      setDeleteDialogOpen(false)
      setUserToDelete(null)
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Gerenciamento de Usuários</h1>
        <p className="text-muted-foreground mt-2">Gerencie usuários pendentes e ative novas contas</p>
      </div>

      <Tabs defaultValue="pending" className="space-y-4">
        <TabsList>
          <TabsTrigger value="pending" className="gap-2">
            <UserCheck className="h-4 w-4" />
            Usuários Pendentes
          </TabsTrigger>
        </TabsList>

        <TabsContent value="pending" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Usuários Aguardando Aprovação</CardTitle>
              <CardDescription>Ative ou rejeite usuários que solicitaram acesso ao sistema</CardDescription>
            </CardHeader>
            <CardContent>
              {loadingPending ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                </div>
              ) : pendingUsers.length === 0 ? (
                <div className="text-center py-8">
                  <CheckCircle className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                  <p className="text-muted-foreground">Nenhum usuário pendente no momento</p>
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
                            <TableCell className="font-medium">{user.name}</TableCell>
                            <TableCell>{user.email}</TableCell>
                            <TableCell>
                              <Badge variant="outline">{user.role}</Badge>
                            </TableCell>
                            <TableCell>{user.createdAt}</TableCell>
                            <TableCell className="text-right">
                              <div className="flex justify-end gap-2">
                                <Button
                                  size="sm"
                                  onClick={() => handleActivate(user.id)}
                                  disabled={activatingId === user.id || deletingId === user.id}
                                >
                                  {activatingId === user.id ? (
                                    <>
                                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                                      Ativando...
                                    </>
                                  ) : (
                                    <>
                                      <CheckCircle className="mr-2 h-4 w-4" />
                                      Ativar
                                    </>
                                  )}
                                </Button>
                                <Button
                                  size="sm"
                                  variant="destructive"
                                  onClick={() => handleDeleteClick(user)}
                                  disabled={activatingId === user.id || deletingId === user.id}
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

                  {/* Pagination */}
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
                          onClick={() => setPendingPage((p) => Math.min(pendingTotalPages - 1, p + 1))}
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
      </Tabs>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Confirmar exclusão</AlertDialogTitle>
            <AlertDialogDescription>
              Tem certeza que deseja excluir o usuário <strong>{userToDelete?.name}</strong>? Esta ação não pode ser
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

import { ApiError, request } from './client'

export interface LoginInput {
  userAccount: string
  password: string
}

export interface CreateAdminInput extends LoginInput {
  nickName: string
}

export interface UserInfoResult {
  userAccount: string
  nickName: string
}

export async function login(input: LoginInput) {
  const token = await request<string>({ url: '/sysUser/login', method: 'POST', data: input }, false)
  if (typeof token !== 'string' || !/^[\w-]+\.[\w-]+\.[\w-]+$/.test(token)) {
    throw new ApiError('登录服务未返回有效令牌，请联系管理员。')
  }
  return token
}

export function getUserInfo() {
  return request<UserInfoResult>({ url: '/sysUser/info', method: 'GET' })
}

export function logout() {
  return request<null>({ url: '/sysUser/logout', method: 'DELETE' })
}

export function createAdmin(input: CreateAdminInput) {
  return request<null>({ url: '/sysUser/add', method: 'POST', data: input })
}

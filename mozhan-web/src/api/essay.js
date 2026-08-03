import request from '@/utils/request'

export function getEssayList(params) {
    return request.get('/essay/list', { params })
}

export function getEssayDetail(id) {
    return request.get(`/essay/detail/${id}`)
}

export function createEssay(data) {
    return request.post('/essay/add', data)
}

export function updateEssay(id, data) {
    return request.put('/essay/update', { ...data, id })
}

export function deleteEssay(id) {
    return request.delete(`/essay/delete/${id}`)
}

export function getMyEssayList(userId, params) {
    return request.get(`/essay/my/${userId}`, { params })
}

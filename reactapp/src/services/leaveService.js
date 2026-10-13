import apiClient from './apiClient';

const handleResponse = (response) => {
    if (response && response.data !== undefined) {
        return response;
    }
    return Promise.reject('Invalid response format');
};

const handleError = (error) => {
    console.error('API call failed:', error);
    return Promise.reject(error);
};

export const getAllLeaves = (page = 0, size = 10) => {
    return apiClient.get(`/leaves/page?page=${page}&size=${size}`)
        .then(handleResponse)
        .catch(handleError);
};

export const getLeaveList = () => {
    return apiClient.get('/leaves')
        .then(handleResponse)
        .catch(handleError);
};

export const getLeaveById = (id) => {
    return apiClient.get(`/leaves/${id}`)
        .then(handleResponse)
        .catch(handleError);
};

export const addLeave = (leave) => {
    return apiClient.post('/leaves', leave)
        .then(handleResponse)
        .catch(handleError);
};

export const updateLeave = (id, leave) => {
    return apiClient.put(`/leaves/${id}`, leave)
        .then(handleResponse)
        .catch(handleError);
};

export const deleteLeave = (id) => {
    return apiClient.delete(`/leaves/${id}`)
        .then(handleResponse)
        .catch(handleError);
};

export const approveLeave = (id) => {
    return apiClient.put(`/leaves/${id}/approve`)
        .then(handleResponse)
        .catch(handleError);
};

export const rejectLeave = (id) => {
    return apiClient.put(`/leaves/${id}/reject`)
        .then(handleResponse)
        .catch(handleError);
};
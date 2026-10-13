import apiClient from './apiClient';

export const getMe = () => {
    return apiClient.get('/auth/me');
};

export const getCurrentUserProfile = () => {
    return apiClient.get('/auth/me');
};

import axiosInstance from './config';

export const uploadCpi = async (cpiFileName: string, cpiContent: File) => {
    var formData = new FormData();
    formData.append('cpiFileName', cpiFileName);
    formData.append('cpiContent', cpiContent);
    return axiosInstance.post(`/api/v1/cpi/`, formData);
};

export const getCpiList = async () => {
    return axiosInstance.get(`/api/v1/cpi/list`);
};

export const getCpiStatus = async (id: string) => {
    //96f6bd8d-64a0-4dfb-aa26-cdcc7c6a2d7c
    return axiosInstance.get(`/api/v1/cpi/status/${id}`);
};

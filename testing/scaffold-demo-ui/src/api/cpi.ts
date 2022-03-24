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
    return axiosInstance.get(`/api/v1/cpi/status/${id}`);
};

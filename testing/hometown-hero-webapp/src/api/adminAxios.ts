import axios, { AxiosInstance } from 'axios';
import { CLUSTERS } from '@/api/apiCall';

//TODO Make dynamic

const authConfig0 = {
    username: 'admin',
    password: 'admin',
};
const authConfig1 = {
    username: 'admin',
    password: 'admin',
};
const authConfig2 = {
    username: 'admin',
    password: 'admin',
};

const adminAxiosInstance: { [key: string]: AxiosInstance } = {
    cluster0: axios.create({
        baseURL: CLUSTERS['cluster0'],
        auth: authConfig0,
    }),
    cluster1: axios.create({
        baseURL: CLUSTERS['cluster1'],
        auth: authConfig1,
    }),
    cluster2: axios.create({
        baseURL: CLUSTERS['cluster2'],
        auth: authConfig2,
    }),
};

export default adminAxiosInstance;

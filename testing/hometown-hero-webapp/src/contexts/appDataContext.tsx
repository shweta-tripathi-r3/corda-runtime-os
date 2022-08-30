import { useEffect, useState } from 'react';

import { Cpi } from '@/models/cpi';
import { VirtualNode } from '@/models/virtualnode';
import adminAxiosInstance from '@/api/adminAxios';
import apiCall from '@/api/apiCall';
import createCtx from './createCtx';
import { REFRESH_NODES_INTERVAL } from '@/config';

type AppDataContextProps = {
    cpiList: Cpi[];
    vNodes: VirtualNode[];
    refreshCpiList: () => void;
    refreshVNodes: () => Promise<VirtualNode[]>;
};

const [useAppDataContext, Provider] = createCtx<AppDataContextProps>();
export default useAppDataContext;

type Props = {
    children?: React.ReactNode;
};

export const AppDataContextProvider: React.FC<Props> = ({ children }) => {
    const [cpiList, setCpiList] = useState<Cpi[]>([]);
    const [vNodes, setVNodes] = useState<VirtualNode[]>([]);

    const refreshCpiList = async () => {
        const response0 = await apiCall({
            method: 'get',
            path: '/api/v1/cpi',
            axiosInstance: adminAxiosInstance['cluster0'],
            cluster: 'cluster0',
        });
        const response1 = await apiCall({
            method: 'get',
            path: '/api/v1/cpi',
            axiosInstance: adminAxiosInstance['cluster1'],
            cluster: 'cluster1',
        });
        const response2 = await apiCall({
            method: 'get',
            path: '/api/v1/cpi',
            axiosInstance: adminAxiosInstance['cluster2'],
            cluster: 'cluster2',
        });
        const allCPIs = [...response0.data.cpis, ...response1.data.cpis, ...response2.data.cpis];

        setCpiList(allCPIs);
    };
    const refreshVNodes = async () => {
        const response0 = await apiCall({
            method: 'get',
            path: '/api/v1/virtualnode',
            axiosInstance: adminAxiosInstance['cluster0'],
            dontTrackRequest: true,
            cluster: 'cluster0',
        });
        const response1 = await apiCall({
            method: 'get',
            path: '/api/v1/virtualnode',
            axiosInstance: adminAxiosInstance['cluster1'],
            dontTrackRequest: true,
            cluster: 'cluster1',
        });
        const response2 = await apiCall({
            method: 'get',
            path: '/api/v1/virtualnode',
            axiosInstance: adminAxiosInstance['cluster2'],
            dontTrackRequest: true,
            cluster: 'cluster2',
        });

        const allNodes = [
            ...response0.data.virtualNodes.map((vn: VirtualNode) => ({ ...vn, cluster: 'cluster0' })),
            ...response1.data.virtualNodes.map((vn: VirtualNode) => ({ ...vn, cluster: 'cluster1' })),
            ...response2.data.virtualNodes.map((vn: VirtualNode) => ({ ...vn, cluster: 'cluster2' })),
        ];
        setVNodes(allNodes);
        return allNodes;
    };

    useEffect(() => {
        refreshCpiList();
        refreshVNodes();

        const interval = setInterval(() => {
            refreshVNodes();
        }, REFRESH_NODES_INTERVAL);

        return () => {
            clearInterval(interval);
        };
    }, []);

    return <Provider value={{ cpiList, vNodes, refreshCpiList, refreshVNodes }}>{children}</Provider>;
};

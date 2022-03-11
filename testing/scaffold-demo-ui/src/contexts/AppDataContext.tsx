import { getCpiList, getCpiStatus } from 'api/cpi';
import { useEffect, useState } from 'react';

import { Cpi } from 'model/cpi';
import { VirtualNode } from 'model/virtualnode';
import { createCtx } from './createCtx';
import { getVNodeList } from 'api/vnode';

type AppDataContextProps = {
    cpiList: Cpi[];
    vNodes: VirtualNode[];
    refreshCpiList: () => void;
    refreshVNodes: () => void;
};

const [useAppDataContext, Provider] = createCtx<AppDataContextProps>();
export default useAppDataContext;

export const AppDataContextProvider = ({ children }) => {
    const [cpiList, setCpiList] = useState<Cpi[]>([]);
    const [vNodes, setVNodes] = useState<VirtualNode[]>([]);

    const refreshCpiList = async () => {
        const response = await getCpiList();
        setCpiList(response.data.cpis);
    };
    const refreshVNodes = async () => {
        const response = await getVNodeList();
        setVNodes(response.data.virtualNodes);
    };

    useEffect(() => {
        refreshCpiList();
    }, []);

    return <Provider value={{ cpiList, vNodes, refreshCpiList, refreshVNodes }}>{children}</Provider>;
};

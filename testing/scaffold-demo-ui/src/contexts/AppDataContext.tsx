import { getCpiList, getCpiStatus } from 'api/cpi';
import { useEffect, useState } from 'react';

import { Cpi } from 'model/cpi';
import { VirtualNode } from 'model/virtualnode';
import { createCtx } from './createCtx';

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
        //const res2 = await getCpiStatus('5cf3e1fd-16b5-44f7-a6c8-c472b998cc09');
    };
    const refreshVNodes = () => {};

    useEffect(() => {
        refreshCpiList();
    }, []);

    return <Provider value={{ cpiList, vNodes, refreshCpiList, refreshVNodes }}>{children}</Provider>;
};

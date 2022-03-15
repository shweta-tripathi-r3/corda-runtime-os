import { IconButton, Typography } from '@material-ui/core';

import RefreshIcon from '@material-ui/icons/Refresh';
import useAppDataContext from 'contexts/AppDataContext';
import { useAppStyles } from 'materialStyles/appStyles';
import useListStyles from 'materialStyles/listStyles';

const VNodeList = () => {
    const appClasses = useAppStyles();
    const listClasses = useListStyles();
    const { vNodes, refreshVNodes } = useAppDataContext();

    return (
        <div style={{ width: 400, display: 'flex', flexDirection: 'column' }}>
            <div style={{ display: 'flex', width: '100%' }}>
                <Typography variant="h4" className={appClasses.contrastText} style={{ marginBottom: 16 }}>
                    V Node List
                </Typography>
                <IconButton onClick={refreshVNodes} className={listClasses.refreshListButton}>
                    <RefreshIcon color="secondary" style={{ width: 32, height: 32 }} />
                </IconButton>
            </div>
            {vNodes.length === 0 && (
                <Typography variant="h6" className={`${appClasses.contrastText} ${listClasses.emptyListText}`}>
                    No V Nodes Created
                </Typography>
            )}
            {vNodes.map((vNode) => {
                return (
                    <div className={listClasses.item}>
                        <div>
                            <Typography className={appClasses.contrastText}>
                                <strong>x500 Name:</strong> {vNode.holdingIdentity.x500Name}
                            </Typography>
                            <Typography className={appClasses.contrastText}>
                                <strong>Group ID:</strong> {vNode.holdingIdentity.groupId}
                            </Typography>
                            <Typography className={appClasses.contrastText}>
                                <strong>Holding ID:</strong> {vNode.holdingIdentity.id}
                            </Typography>
                            <Typography className={appClasses.contrastText}>
                                <strong>Cpi : </strong>
                                {vNode.cpiIdentifier.name}
                            </Typography>
                        </div>
                    </div>
                );
            })}
        </div>
    );
};

export default VNodeList;

import { IconButton, Typography } from '@material-ui/core';

import RefreshIcon from '@material-ui/icons/Refresh';
import useAppDataContext from 'contexts/AppDataContext';
import { useAppStyles } from 'materialStyles/appStyles';

const VNodeList = () => {
    const appClasses = useAppStyles();
    const { vNodes, refreshVNodes } = useAppDataContext();

    return (
        <div style={{ width: 350, display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div style={{ display: 'flex', width: '100%' }}>
                <Typography variant="h4" className={appClasses.contrastText} style={{ marginBottom: 16 }}>
                    V Nodes List
                </Typography>
                <IconButton
                    onClick={refreshVNodes}
                    style={{ marginRight: 6, marginLeft: 'auto', width: 32, height: 32 }}
                >
                    <RefreshIcon color="secondary" style={{ width: 32, height: 32 }} />
                </IconButton>
            </div>
            {vNodes.length === 0 && (
                <Typography
                    variant="h6"
                    className={appClasses.contrastText}
                    style={{ marginBottom: 16, marginLeft: 6, opacity: 0.8 }}
                >
                    No V Nodes Created
                </Typography>
            )}
        </div>
    );
};

export default VNodeList;

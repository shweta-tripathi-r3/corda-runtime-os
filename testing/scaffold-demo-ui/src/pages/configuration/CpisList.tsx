import { Divider, IconButton, Tooltip, Typography, withStyles } from '@material-ui/core';

import AddIcon from '@material-ui/icons/Add';
import AdjustIcon from '@material-ui/icons/Adjust';
import { Cpi } from 'model/cpi';
import RefreshIcon from '@material-ui/icons/Refresh';
import useAppDataContext from 'contexts/AppDataContext';
import { useAppStyles } from 'materialStyles/appStyles';

const CustomToolTip = withStyles((theme) => ({
    tooltip: {
        backgroundColor: theme.palette.common.white,
        color: 'rgba(0, 0, 0, 0.87)',
        boxShadow: theme.shadows[1],
        fontSize: 16,
    },
}))(Tooltip);

type Props = {
    selectCpi: (cpi: Cpi) => void;
};

const CpisList: React.FunctionComponent<Props> = ({ selectCpi }) => {
    const appClasses = useAppStyles();
    const { cpiList, refreshCpiList } = useAppDataContext();
    return (
        <>
            <div style={{ display: 'flex', width: '100%' }}>
                <Typography variant="h4" className={appClasses.contrastText} style={{ marginBottom: 16 }}>
                    CPIs List
                </Typography>
                <IconButton
                    onClick={refreshCpiList}
                    style={{ marginRight: 6, marginLeft: 'auto', width: 32, height: 32 }}
                >
                    <RefreshIcon color="secondary" style={{ width: 32, height: 32 }} />
                </IconButton>
            </div>
            {cpiList.length === 0 && (
                <Typography
                    variant="h6"
                    className={appClasses.contrastText}
                    style={{ marginBottom: 16, marginLeft: 6, opacity: 0.8 }}
                >
                    No CPIs Uploaded
                </Typography>
            )}
            {cpiList.map((cpi) => {
                return (
                    <div style={{ border: '1px grey solid', padding: 8, borderRadius: 8, display: 'flex' }}>
                        <div>
                            <Typography className={appClasses.contrastText}>Name: {cpi.id.cpiName}</Typography>
                            <Typography className={appClasses.contrastText}>Version: {cpi.id.cpiVersion}</Typography>
                            <Divider
                                color="secondary"
                                style={{ backgroundColor: 'white', width: '80%', marginTop: 8, marginBottom: 8 }}
                            />
                            <Typography className={appClasses.contrastText}>CPKS:</Typography>
                            {cpi.cpks.map((cpk) => (
                                <div style={{ border: '1px grey', padding: 8, borderRadius: 8 }}>
                                    <Typography className={appClasses.contrastText}>Name: {cpk.id.name}</Typography>
                                    <Typography className={appClasses.contrastText}>
                                        Main Bundle: {cpk.mainBundle}
                                    </Typography>
                                </div>
                            ))}
                        </div>
                        <CustomToolTip title={'Create V Node'} placement={'top'}>
                            <IconButton
                                style={{ height: 'fit-content' }}
                                onClick={() => {
                                    selectCpi(cpi);
                                }}
                            >
                                <div style={{ display: 'flex' }}>
                                    <AddIcon color="secondary" />
                                    <AdjustIcon color="secondary" />
                                </div>
                            </IconButton>
                        </CustomToolTip>
                    </div>
                );
            })}
        </>
    );
};

export default CpisList;

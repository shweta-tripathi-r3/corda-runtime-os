import { Divider, IconButton, Tooltip, Typography, withStyles } from '@material-ui/core';

import AddIcon from '@material-ui/icons/Add';
import AdjustIcon from '@material-ui/icons/Adjust';
import { Cpi } from 'model/cpi';
import RefreshIcon from '@material-ui/icons/Refresh';
import useAppDataContext from 'contexts/AppDataContext';
import { useAppStyles } from 'materialStyles/appStyles';
import useListStyles from 'materialStyles/listStyles';

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
    const listClasses = useListStyles();
    const { cpiList, refreshCpiList } = useAppDataContext();
    return (
        <>
            <div style={{ display: 'flex', width: '100%' }}>
                <Typography variant="h4" className={appClasses.contrastText} style={{ marginBottom: 16 }}>
                    CPI List
                </Typography>
                <IconButton onClick={refreshCpiList} className={listClasses.refreshListButton}>
                    <RefreshIcon color="secondary" style={{ width: 32, height: 32 }} />
                </IconButton>
            </div>
            {cpiList.length === 0 && (
                <Typography variant="h6" className={`${appClasses.contrastText} ${listClasses.emptyListText}`}>
                    No CPIs Uploaded
                </Typography>
            )}
            {cpiList.map((cpi) => {
                return (
                    <div className={listClasses.item}>
                        <div>
                            <Typography className={appClasses.contrastText}>
                                {' '}
                                <strong>Name: </strong> {cpi.id.cpiName}
                            </Typography>
                            <Typography className={appClasses.contrastText}>
                                <strong>Version:</strong> {cpi.id.cpiVersion}
                            </Typography>
                            <Divider className={listClasses.divider} />
                            <Typography className={appClasses.contrastText}>
                                <strong>CPKS:</strong>
                            </Typography>
                            {cpi.cpks.map((cpk) => (
                                <div style={{ border: '1px grey', padding: 8, borderRadius: 8 }}>
                                    <Typography className={appClasses.contrastText}>
                                        <strong>Name:</strong> {cpk.id.name}
                                    </Typography>
                                    <Typography className={appClasses.contrastText}>
                                        <strong> Main Bundle:</strong> {cpk.mainBundle}
                                    </Typography>
                                </div>
                            ))}
                        </div>
                        <CustomToolTip title={'Create V Node'} placement={'top'}>
                            <IconButton
                                style={{ height: 20, marginTop: 6 }}
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

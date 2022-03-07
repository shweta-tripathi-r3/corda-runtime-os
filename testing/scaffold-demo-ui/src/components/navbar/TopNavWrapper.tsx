import { AppBar, Paper, Toolbar, Typography } from '@material-ui/core';

import { useAppStyles } from 'materialStyles/appStyles';
import { useTopNavWrapperStyles } from 'materialStyles/topNavWrapperStyles';

interface Props {
    navButtons: React.ReactNode;
    icon: React.ReactNode;
    additionalButtons?: React.ReactNode;
}

const TopNavWrapper: React.FC<Props> = ({ navButtons, icon, additionalButtons }) => {
    const classes = useTopNavWrapperStyles();

    return (
        <Paper elevation={5} className={classes.grow}>
            <AppBar
                style={{
                    borderBottomRightRadius: 15,
                    borderBottomLeftRadius: 15,
                }}
                className={`${classes.appBar} ${useAppStyles().shadow}`}
                elevation={0}
                position="static"
            >
                <Toolbar>
                    {icon}

                    <Typography className={classes.title} variant="h6" noWrap>
                        Scaffold Demo
                    </Typography>
                    <Typography className={classes.subTitle} variant="body1" noWrap>
                        Powered By
                    </Typography>
                    <img className={classes.cordaImg} src={`/cordawht.png`} height={30} width={75} alt="Corda R3" />
                    <Typography className={classes.title} style={{ marginLeft: 4, fontSize: 24 }}>
                        5
                    </Typography>
                    <div className={classes.grow}></div>
                    {additionalButtons}
                    <div className={classes.sectionDesktop}>
                        <img src={'/brandedIcon.png'} alt={'r3'} height={35} />
                    </div>
                </Toolbar>
                <div className={classes.navButtonContainer}>{navButtons}</div>
            </AppBar>
        </Paper>
    );
};

export default TopNavWrapper;

import { Divider, Paper } from '@material-ui/core';
import { Theme, createStyles, makeStyles } from '@material-ui/core';

type Props = {
    copyright: string;
};

const useStyles = makeStyles(
    (theme: Theme) =>
        createStyles({
            footer: {
                position: 'fixed',
                width: '100%',
                textAlign: 'center',
                backgroundColor: theme.palette.primary.dark,
                color: theme.palette.primary.contrastText,
                display: 'flex',
                height: 70,
                paddingTop: 20,
                borderBottomLeftRadius: 0,
                borderBottomRightRadius: 0,
                
                bottom: 0,
            },
            children: {
                marginLeft: 'auto',
                marginRight: 50,
            },
            copyright: {
                marginTop: 15,
                marginLeft: 30,
            },
            cordaNode: { marginTop: 15, marginLeft: 60 },
            footWrapper: {
                width: '100%',
            },
            divider: {
                backgroundColor: theme.palette.secondary.main,
                color: theme.palette.secondary.main,
            },
        }),
    { index: 1 }
);

const Footer: React.FC<Props> = ({ copyright, children }) => {
    const styles = useStyles();
    return (
        <>
            <Paper elevation={10} className={styles.footer}>
                <Divider className={styles.divider} />
                <span className={styles.copyright}>{copyright}</span>
            </Paper>
        </>
    );
};

export default Footer;

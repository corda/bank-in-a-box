import { Card, CardContent, Divider, Fade, Paper, Typography } from '@material-ui/core';

import React from 'react';
import { Transaction } from '../../store/types';
import { useInfoDisplayStyles } from '../MaterialStyles/InfoDisplayStyles';
import { useTranslation } from 'react-i18next';

type Props = {
    transaction: Transaction;
};

const TransactionDetails: React.FC<Props> = (props) => {
    const { t } = useTranslation('common');
    const { transaction } = props;
    const infoDisplayStyles = useInfoDisplayStyles();

    return (
        <Fade in={true}>
            <Paper elevation={3} className={infoDisplayStyles.infoDisplay}>
                <div className={infoDisplayStyles.column}>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:transaction.transactionId')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {transaction.txId}
                            </Typography>
                        </CardContent>
                    </Card>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:transaction.amount')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {transaction.amount}
                            </Typography>
                        </CardContent>
                    </Card>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:commonText.currency')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography className={infoDisplayStyles.cardText} variant="body1" component="p">
                                {transaction.currency}
                            </Typography>
                        </CardContent>
                    </Card>
                </div>
                <div className={infoDisplayStyles.column}>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:transaction.type')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography variant="body1" component="p">
                                {transaction.txType}
                            </Typography>
                        </CardContent>
                    </Card>
                    <Card className={infoDisplayStyles.card}>
                        <CardContent>
                            <Typography variant="h5" component="h2">
                                {t('common:transaction.transactionDate')}
                            </Typography>
                            <br />
                            <Divider />
                            <br />
                            <br />
                            <Typography variant="body1" component="p">
                                {transaction.txDate}
                            </Typography>
                        </CardContent>
                    </Card>
                </div>
            </Paper>
        </Fade>
    );
};

export default TransactionDetails;

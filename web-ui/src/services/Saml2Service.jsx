import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"



export const saml2IdpSso = async (samlIssuer, samlRequestId) => {

    return apiRequestWrapper(() => httpWrapper.get('/api/auth/saml2/idp/sso', {
        params: {
            samlIssuer: samlIssuer,
            samlRequestId: samlRequestId,
        }
    }), { raw: true })
}
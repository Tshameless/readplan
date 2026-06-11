export const hasRequiredPermission = (
  ownedPermissions: string[],
  requiredPermission?: string | string[],
) => {
  if (!requiredPermission) {
    return true;
  }

  const requiredList = Array.isArray(requiredPermission) ? requiredPermission : [requiredPermission];
  return requiredList.every((permission) => ownedPermissions.includes(permission));
};
